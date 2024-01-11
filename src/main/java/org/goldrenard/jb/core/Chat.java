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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.goldrenard.jb.configuration.Constants;
import org.goldrenard.jb.model.History;
import org.goldrenard.jb.model.Predicates;
import org.goldrenard.jb.model.Request;
import org.goldrenard.jb.utils.IOUtils;
import org.goldrenard.jb.utils.JapaneseUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Getter;
import lombok.Setter;

/**
 * Class encapsulating a chat session between a bot and a client
 */
@Getter
@Setter
public class Chat {

    private static final Logger log = LoggerFactory.getLogger(Chat.class);

    private final Bot bot;
    private final TripleStore tripleStore;
    private boolean doWrites;
    private String customerId;
    private History<History> thatHistory;
    private History<String> requestHistory;
    private History<String> responseHistory;
    private History<String> inputHistory;
    private Predicates predicates;

    /**
     * Constructor  (defualt customer ID)
     *
     * @param bot the bot to chat with
     */
    public Chat(final Bot bot) {
        this(bot, true, "0");
    }

    public Chat(final Bot bot, final boolean doWrites) {
        this(bot, doWrites, "0");
    }

    /**
     * Constructor
     *
     * @param bot        bot to chat with
     * @param customerId unique customer identifier
     */
    public Chat(final Bot bot, final boolean doWrites, final String customerId) {
        this.customerId = customerId;
        this.bot = bot;
        this.tripleStore = new TripleStore("anon", bot);
        this.doWrites = doWrites;
        final int maxHistory = bot.getConfiguration().getMaxHistory();
        this.thatHistory = new History<>(maxHistory, "that");
        this.requestHistory = new History<>(maxHistory, "request");
        this.responseHistory = new History<>(maxHistory, "response");
        this.inputHistory = new History<>(maxHistory, "input");
        final History<String> contextThatHistory = new History<>(maxHistory);
        contextThatHistory.add(Constants.default_that);
        this.thatHistory.add(contextThatHistory);
        this.predicates = new Predicates(bot);
        this.predicates.put("topic", Constants.default_topic);
        this.predicates.put("jsenabled", Constants.js_enabled);
        if (log.isTraceEnabled()) {
            log.trace("Chat Session Created for bot {}", bot.getName());
        }
        this.addPredicates();
        this.addTriples();
    }

    /**
     * Load all predicate defaults
     */
    private void addPredicates() {
        try {
            this.predicates.getPredicateDefaults(this.bot.getConfigPath() + "/predicates.txt");
        } catch (final Exception e) {
            log.warn("Error reading predicates", e);
        }
    }

    /**
     * Load Triple Store knowledge base
     */
    private int addTriples() {
        int count = 0;
        final String fileName = this.bot.getConfigPath() + "/triples.txt";
        if (log.isTraceEnabled()) {
            log.trace("Loading Triples from {}", fileName);
        }
        final File f = new File(fileName);
        if (f.exists()) {
            try (InputStream is = new FileInputStream(f)) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                    String strLine;
                    //Read File Line By Line
                    while ((strLine = br.readLine()) != null) {
                        final String[] triple = strLine.split(":");
                        if (triple.length >= 3) {
                            final String subject = triple[0];
                            final String predicate = triple[1];
                            final String object = triple[2];
                            this.tripleStore.addTriple(subject, predicate, object);
                            count++;
                        }
                    }
                }
            } catch (final Exception e) {
                log.warn("Error reading triples", e);
            }
        }
        if (log.isTraceEnabled()) {
            log.trace("Loaded {} triples", count);
        }
        return count;
    }

    /**
     * Chat session terminal interaction
     */
    private void chat() {
        try {
            String request = "SET PREDICATES"; // TODO why it is executed here?
            String response = this.multisentenceRespond(request);
            while (!"quit".equals(request)) {
                log.info("Human: ");
                request = IOUtils.readInputTextLine();
                response = this.multisentenceRespond(request);
                log.info("Robot: {}", response);
            }
        } catch (final Exception e) {
            log.warn("Error: ", e);
        }
    }

    /**
     * Return bot response to a single sentence input given conversation context
     *
     * @param input              client input
     * @param that               bot's last sentence
     * @param topic              current topic
     * @param contextThatHistory history of "that" values for this request/response interaction
     * @return bot's reply
     */
    private String respond(final Request request, String input, final String that, final String topic, final History<String> contextThatHistory) {
        boolean repetition = true;
        for (int i = 0; i < this.bot.getConfiguration().getRepetitionCount(); i++) {
            if (this.inputHistory.get(i) == null || !input.toUpperCase().equals(this.inputHistory.get(i).toUpperCase())) {
                repetition = false;
            }
        }
        if (input.equals(Constants.null_input)) {
            repetition = false;
        }
        this.inputHistory.add(input);
        if (repetition) {
            input = Constants.repetition_detected;
        }

        String response;

        response = this.bot.getProcessor().respond(request, input, that, topic, this);
        String normResponse = this.bot.getPreProcessor().normalize(response);
        if (this.bot.getConfiguration().isJpTokenize()) {
            normResponse = JapaneseUtils.tokenizeSentence(normResponse);
        }
        final String sentences[] = this.bot.getPreProcessor().sentenceSplit(normResponse);
        for (String s : sentences) {
            if (s.trim().equals("")) {
                s = Constants.default_that;
            }
            contextThatHistory.add(s);
        }
        return response.trim();
    }

    /**
     * Return bot response given an input and a history of "that" for the current conversational interaction
     *
     * @param input              client input
     * @param contextThatHistory history of "that" values for this request/response interaction
     * @return bot's reply
     */
    private String respond(final Request request, final String input, final History<String> contextThatHistory) {
        final History hist = this.thatHistory.get(0);
        final String that = hist != null ? hist.getString(0) : Constants.default_that;
        return this.respond(request, input, that, this.predicates.get("topic"), contextThatHistory);
    }

    public String multisentenceRespond(final String request) {
        return this.multisentenceRespond(Request.builder().input(request).build());
    }

    /**
     * return a compound response to a multiple-sentence request. "Multiple" means one or more.
     *
     * @param request client's multiple-sentence input
     * @return Response
     */
    public String multisentenceRespond(final Request request) {
        final StringBuilder response = new StringBuilder();
        try {
            String normalized = this.bot.getPreProcessor().normalize(request.getInput());
            if (this.bot.getConfiguration().isJpTokenize()) {
                normalized = JapaneseUtils.tokenizeSentence(normalized);
            }
            final String sentences[] = this.bot.getPreProcessor().sentenceSplit(normalized);
            final History<String> contextThatHistory = new History<>(this.bot.getConfiguration().getMaxHistory(), "contextThat");
            for (final String sentence : sentences) {
                final String reply = this.respond(request, sentence, contextThatHistory);
                response.append(" ").append(reply.trim());
            }

            String result = response.toString();
            this.requestHistory.add(request.getInput());
            this.responseHistory.add(result);
            this.thatHistory.add(contextThatHistory);
            result = result.replaceAll("[\n]+", "\n").trim();
            if (this.doWrites) {
                this.bot.writeLearnfIFCategories();
            }
            return result;
        } catch (final Exception e) {
            log.error("Error: ", e);
        }
        return this.bot.getConfiguration().getLanguage().getErrorResponse();
    }
}
