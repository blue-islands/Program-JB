/*
 * This file is part of Program JB.
 *
 * Program JB is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * Program JB is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with Program JB. If not, see <http://www.gnu.org/licenses/>.
 */
package org.goldrenard.jb.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.goldrenard.jb.model.Request;
import org.goldrenard.jb.utils.CalendarUtils;
import org.goldrenard.jb.utils.NetworkUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// https://api.duckduckgo.com/?q=query&format=json&pretty=1
public class Sraix {

    private static final Logger log = LoggerFactory.getLogger(Sraix.class);

    public final static String sraix_failed = "SRAIXFAILED";
    public final static String sraix_no_hint = "nohint";
    public final static String sraix_event_hint = "event";
    public final static String sraix_pic_hint = "pic";
    public final static String sraix_shopping_hint = "shopping";

    private final static Map<String, String> custIdMap = new ConcurrentHashMap<>();

    public static String sraix(final Request request, final Chat chatSession, final Bot bot, final String input, final String defaultResponse, final String hint, final String host, final String botid, final String apiKey, final String limit) {
        String response;
        if (!bot.getConfiguration().isEnableNetworkConnection()) {
            response = sraix_failed;
        } else if (host != null && botid != null) {
            response = sraixPandorabots(input, host, botid);
        } else {
            response = sraixPannous(request, input, hint, chatSession);
        }
        log.debug("Sraix: response = {} defaultResponse = {}", response, defaultResponse);
        if (response.equals(sraix_failed)) {
            if (chatSession != null && defaultResponse == null) {
                response = bot.getProcessor().respond(request, sraix_failed, "nothing",
                        "nothing", chatSession);
            } else if (defaultResponse != null) {
                response = defaultResponse;
            }
        }
        return response;
    }

    private static String sraixPandorabots(final String input, final String host, final String botid) {
        final String responseContent = pandorabotsRequest(input, host, botid);
        if (responseContent == null) {
            return sraix_failed;
        }
        return pandorabotsResponse(responseContent, host, botid);
    }

    private static String pandorabotsRequest(final String input, final String host, final String botid) {
        try {
            final String key = host + ":" + botid;
            final String custid = custIdMap.getOrDefault(key, "0");
            final String spec = NetworkUtils.spec(host, botid, custid, input);
            if (log.isTraceEnabled()) {
                log.trace("Spec = {}", spec);
            }
            final String responseContent = NetworkUtils.responseContent(spec);
            if (log.isTraceEnabled()) {
                log.trace("Sraix: Response={}", responseContent);
            }
            return responseContent;
        } catch (final Exception e) {
            log.error("Error:", e);
        }
        return null;
    }

    public static String pandorabotsResponse(final String sraixResponse, final String host, final String botid) {
        String botResponse = sraix_failed;
        try {
            int n1 = sraixResponse.indexOf("<that>");
            int n2 = sraixResponse.indexOf("</that>");

            if (n2 > n1) {
                botResponse = sraixResponse.substring(n1 + "<that>".length(), n2);
            }
            n1 = sraixResponse.indexOf("custid=");
            if (n1 > 0) {
                String custid = sraixResponse.substring(n1 + "custid=\"".length(), sraixResponse.length());
                n2 = custid.indexOf("\"");
                custid = n2 > 0 ? custid.substring(0, n2) : "0";
                final String key = host + ":" + botid;
                custIdMap.put(key, custid);
            }
            if (botResponse.endsWith(".")) {
                botResponse = botResponse.substring(0, botResponse.length() - 1);   // snnoying Pandorabots extra "."
            }
        } catch (final Exception e) {
            log.error("Error:", e);
        }
        return botResponse;
    }

    public static String sraixPannous(final Request request, String input, String hint, final Chat chatSession) {
        try {
            final String rawInput = input;
            if (hint == null) {
                hint = sraix_no_hint;
            }
            input = " " + input + " ";
            input = input.replace(" point ", ".");
            input = input.replace(" rparen ", ")");
            input = input.replace(" lparen ", "(");
            input = input.replace(" slash ", "/");
            input = input.replace(" star ", "*");
            input = input.replace(" dash ", "-");
            // input = chatSession.bot.preProcessor.denormalize(input);
            input = input.trim();
            input = input.replace(" ", "+");
            final int offset = CalendarUtils.timeZoneOffset();
            final String locationString = "";

            final String apiKey = chatSession.getBot().getConfiguration().getPannousApiKey();
            final String login = chatSession.getBot().getConfiguration().getPannousLogin();

            // https://weannie.pannous.com/api?input=when+is+daylight+savings+time+in+the+us&locale=en_US&login=pandorabots&ip=169.254.178.212&botid=0&key=CKNgaaVLvNcLhDupiJ1R8vtPzHzWc8mhIQDFSYWj&exclude=Dialogues,ChatBot&out=json
            // exclude=Dialogues,ChatBot&out=json&clientFeatures=show-images,reminder,say&debug=true
            final String url = "http://ask.pannous.com/api?input=" + input + "&locale=en_US&timeZone=" + offset +
                    locationString + "&login=" + login + "&ip=" +
                    NetworkUtils.localIPAddress() + "&botid=0&key=" + apiKey +
                    "&exclude=Dialogues,ChatBot&out=json&clientFeatures=show-images,reminder,say&debug=true";
            if (log.isTraceEnabled()) {
                log.trace("in Sraix.sraixPannous, url: '{}'", url);
            }
            final String page = NetworkUtils.responseContent(url);
            String text = "";
            String imgRef = "";
            String urlRef = "";
            if (page.isEmpty()) {
                text = sraix_failed;
            } else {
                final JSONArray outputJson = new JSONObject(page).getJSONArray("output");
                if (outputJson.length() == 0) {
                    text = sraix_failed;
                } else {
                    final JSONObject firstHandler = outputJson.getJSONObject(0);
                    final JSONObject actions = firstHandler.getJSONObject("actions");
                    if (actions.has("reminder")) {
                        final Object obj = actions.get("reminder");
                        if (obj instanceof JSONObject) {
                            final JSONObject sObj = (JSONObject) obj;
                            String date = sObj.getString("date");
                            date = date.substring(0, "2012-10-24T14:32".length());
                            final String duration = sObj.getString("duration");

                            final Pattern datePattern = Pattern.compile("(.*)-(.*)-(.*)T(.*):(.*)");
                            final Matcher m = datePattern.matcher(date);
                            String year = "", month = "", day = "", hour = "", minute = "";
                            if (m.matches()) {
                                year = m.group(1);
                                month = String.valueOf(Integer.parseInt(m.group(2)) - 1);
                                day = m.group(3);

                                hour = m.group(4);
                                minute = m.group(5);
                                text = "<year>" + year + "</year>" +
                                        "<month>" + month + "</month>" +
                                        "<day>" + day + "</day>" +
                                        "<hour>" + hour + "</hour>" +
                                        "<minute>" + minute + "</minute>" +
                                        "<duration>" + duration + "</duration>";
                            } else {
                                text = chatSession.getBot().getConfiguration().getLanguage().getScheduleError();
                            }
                        }
                    } else if (actions.has("say") && !hint.equals(sraix_pic_hint) && !hint.equals(sraix_shopping_hint)) {
                        final Object obj = actions.get("say");
                        if (obj instanceof JSONObject) {
                            final JSONObject sObj = (JSONObject) obj;
                            final StringBuilder builder = new StringBuilder(sObj.getString("text"));
                            if (sObj.has("moreText")) {
                                final JSONArray arr = sObj.getJSONArray("moreText");
                                for (int i = 0; i < arr.length(); i++) {
                                    builder.append(" ").append(arr.getString(i));
                                }
                            }
                            text = builder.toString();
                        } else {
                            text = obj.toString();
                        }
                    }
                    if (actions.has("show") && !text.contains("Wolfram")
                            && actions.getJSONObject("show").has("images")) {
                        final JSONArray arr = actions.getJSONObject("show").getJSONArray("images");
                        final int i = (int) (arr.length() * Math.random());
                        imgRef = arr.getString(i);
                        if (imgRef.startsWith("//")) {
                            imgRef = "http:" + imgRef;
                        }
                        imgRef = "<a href=\"" + imgRef + "\"><img src=\"" + imgRef + "\"/></a>";
                    }
                    if (hint.equals(sraix_shopping_hint) && actions.has("open") && actions.getJSONObject("open").has("url")) {
                        urlRef = "<oob><url>" + actions.getJSONObject("open").getString("url") + "</oob></url>";
                    }
                }
                if (hint.equals(sraix_event_hint) && !text.startsWith("<year>")) {
                    return sraix_failed;
                } else if (text.equals(sraix_failed)) {
                    return chatSession.getBot().getProcessor().respond(request, sraix_failed, "nothing", "nothing", chatSession);
                } else {
                    text = text.replace("&#39;", "'");
                    text = text.replace("&apos;", "'");
                    text = text.replaceAll("\\[(.*)\\]", "");
                    String[] sentences;
                    sentences = text.split("\\. ");
                    final StringBuilder builder = new StringBuilder(sentences[0]);
                    for (int i = 1; i < sentences.length; i++) {
                        if (builder.length() < 500) {
                            builder.append(". ").append(sentences[i]);
                        }
                    }
                    String clippedPage = builder.toString() + " " + imgRef + " " + urlRef;
                    clippedPage = clippedPage.trim();
                    return clippedPage;
                }
            }
        } catch (final Exception e) {
            log.error("Error:", e);
        }
        return sraix_failed;
    }
}
