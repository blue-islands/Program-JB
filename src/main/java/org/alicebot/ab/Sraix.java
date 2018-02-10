/* Program AB Reference AIML 2.0 implementation
        Copyright (C) 2013 ALICE A.I. Foundation
        Contact: info@alicebot.org

        This library is free software; you can redistribute it and/or
        modify it under the terms of the GNU Library General Public
        License as published by the Free Software Foundation; either
        version 2 of the License, or (at your option) any later version.

        This library is distributed in the hope that it will be useful,
        but WITHOUT ANY WARRANTY; without even the implied warranty of
        MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
        Library General Public License for more details.

        You should have received a copy of the GNU Library General Public
        License along with this library; if not, write to the
        Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
        Boston, MA  02110-1301, USA.
*/
package org.alicebot.ab;

import org.alicebot.ab.utils.CalendarUtils;
import org.alicebot.ab.utils.NetworkUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Sraix {

    private static final Logger log = LoggerFactory.getLogger(Sraix.class);

    private final static Map<String, String> custIdMap = new ConcurrentHashMap<>();

    public static String sraix(Chat chatSession, String input, String defaultResponse, String hint, String host, String botid, String apiKey, String limit) {
        String response;
        if (!MagicBooleans.enable_network_connection) {
            response = MagicStrings.sraix_failed;
        } else if (host != null && botid != null) {
            response = sraixPandorabots(input, chatSession, host, botid);
        } else {
            response = sraixPannous(input, hint, chatSession);
        }
        log.debug("Sraix: response = {} defaultResponse = {}", response, defaultResponse);
        if (response.equals(MagicStrings.sraix_failed)) {
            if (chatSession != null && defaultResponse == null) {
                response = chatSession.getBot().getProcessor().respond(MagicStrings.sraix_failed, "nothing",
                        "nothing", chatSession);
            } else if (defaultResponse != null) {
                response = defaultResponse;
            }
        }
        return response;
    }

    public static String sraixPandorabots(String input, Chat chatSession, String host, String botid) {
        String responseContent = pandorabotsRequest(input, host, botid);
        if (responseContent == null) {
            return MagicStrings.sraix_failed;
        }
        return pandorabotsResponse(responseContent, chatSession, host, botid);
    }

    public static String pandorabotsRequest(String input, String host, String botid) {
        try {
            String key = host + ":" + botid;
            String custid = custIdMap.getOrDefault(key, "0");
            String spec = NetworkUtils.spec(host, botid, custid, input);
            if (log.isTraceEnabled()) {
                log.trace("Spec = {}", spec);
            }
            String responseContent = NetworkUtils.responseContent(spec);
            if (log.isTraceEnabled()) {
                log.trace("Sraix: Response={}", responseContent);
            }
            return responseContent;
        } catch (Exception e) {
            log.error("Error:", e);
        }
        return null;
    }

    public static String pandorabotsResponse(String sraixResponse, Chat chatSession, String host, String botid) {
        String botResponse = MagicStrings.sraix_failed;
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
                String key = host + ":" + botid;
                custIdMap.put(key, custid);
            }
            if (botResponse.endsWith(".")) {
                botResponse = botResponse.substring(0, botResponse.length() - 1);   // snnoying Pandorabots extra "."
            }
        } catch (Exception e) {
            log.error("Error:", e);
        }
        return botResponse;
    }

    public static String sraixPannous(String input, String hint, Chat chatSession) {
        try {
            String rawInput = input;
            if (hint == null) {
                hint = MagicStrings.sraix_no_hint;
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
            int offset = CalendarUtils.timeZoneOffset();
            String locationString = "";

            // https://weannie.pannous.com/api?input=when+is+daylight+savings+time+in+the+us&locale=en_US&login=pandorabots&ip=169.254.178.212&botid=0&key=CKNgaaVLvNcLhDupiJ1R8vtPzHzWc8mhIQDFSYWj&exclude=Dialogues,ChatBot&out=json
            // exclude=Dialogues,ChatBot&out=json&clientFeatures=show-images,reminder,say&debug=true
            String url = "http://ask.pannous.com/api?input=" + input + "&locale=en_US&timeZone=" + offset +
                    locationString + "&login=" + MagicStrings.pannous_login + "&ip=" +
                    NetworkUtils.localIPAddress() + "&botid=0&key=" + MagicStrings.pannous_api_key +
                    "&exclude=Dialogues,ChatBot&out=json&clientFeatures=show-images,reminder,say&debug=true";
            if (log.isTraceEnabled()) {
                log.trace("in Sraix.sraixPannous, url: '{}'", url);
            }
            String page = NetworkUtils.responseContent(url);
            String text = "";
            String imgRef = "";
            String urlRef = "";
            if (page.isEmpty()) {
                text = MagicStrings.sraix_failed;
            } else {
                JSONArray outputJson = new JSONObject(page).getJSONArray("output");
                if (outputJson.length() == 0) {
                    text = MagicStrings.sraix_failed;
                } else {
                    JSONObject firstHandler = outputJson.getJSONObject(0);
                    JSONObject actions = firstHandler.getJSONObject("actions");
                    if (actions.has("reminder")) {
                        Object obj = actions.get("reminder");
                        if (obj instanceof JSONObject) {
                            JSONObject sObj = (JSONObject) obj;
                            String date = sObj.getString("date");
                            date = date.substring(0, "2012-10-24T14:32".length());
                            String duration = sObj.getString("duration");

                            Pattern datePattern = Pattern.compile("(.*)-(.*)-(.*)T(.*):(.*)");
                            Matcher m = datePattern.matcher(date);
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
                                text = MagicStrings.schedule_error;
                            }
                        }
                    } else if (actions.has("say") && !hint.equals(MagicStrings.sraix_pic_hint) && !hint.equals(MagicStrings.sraix_shopping_hint)) {
                        Object obj = actions.get("say");
                        if (obj instanceof JSONObject) {
                            JSONObject sObj = (JSONObject) obj;
                            StringBuilder builder = new StringBuilder(sObj.getString("text"));
                            if (sObj.has("moreText")) {
                                JSONArray arr = sObj.getJSONArray("moreText");
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
                        JSONArray arr = actions.getJSONObject("show").getJSONArray("images");
                        int i = (int) (arr.length() * Math.random());
                        imgRef = arr.getString(i);
                        if (imgRef.startsWith("//")) {
                            imgRef = "http:" + imgRef;
                        }
                        imgRef = "<a href=\"" + imgRef + "\"><img src=\"" + imgRef + "\"/></a>";
                    }
                    if (hint.equals(MagicStrings.sraix_shopping_hint) && actions.has("open") && actions.getJSONObject("open").has("url")) {
                        urlRef = "<oob><url>" + actions.getJSONObject("open").getString("url") + "</oob></url>";
                    }
                }
                if (hint.equals(MagicStrings.sraix_event_hint) && !text.startsWith("<year>")) {
                    return MagicStrings.sraix_failed;
                } else if (text.equals(MagicStrings.sraix_failed)) {
                    return chatSession.getBot().getProcessor().respond(MagicStrings.sraix_failed, "nothing", "nothing", chatSession);
                } else {
                    text = text.replace("&#39;", "'");
                    text = text.replace("&apos;", "'");
                    text = text.replaceAll("\\[(.*)\\]", "");
                    String[] sentences;
                    sentences = text.split("\\. ");
                    StringBuilder builder = new StringBuilder(sentences[0]);
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
        } catch (Exception e) {
            log.error("Error:", e);
        }
        return MagicStrings.sraix_failed;
    }
}
