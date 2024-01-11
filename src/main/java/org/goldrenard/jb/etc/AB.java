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
package org.goldrenard.jb.etc;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.goldrenard.jb.configuration.BotConfiguration;
import org.goldrenard.jb.configuration.Constants;
import org.goldrenard.jb.core.Bot;
import org.goldrenard.jb.core.Graphmaster;
import org.goldrenard.jb.core.NodemapperOperator;
import org.goldrenard.jb.model.AIMLSet;
import org.goldrenard.jb.model.Category;
import org.goldrenard.jb.model.Nodemapper;
import org.goldrenard.jb.utils.IOUtils;
import org.goldrenard.jb.utils.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Experimental class that analyzes log data and suggests
 * new AIML patterns.
 */
public class AB {

    public static int node_activation_cnt = 4;  // minimum number of activations to suggest atomic pattern
    public static int node_size = 4;  // minimum number of branches to suggest wildcard pattern
    public static int displayed_input_sample_size = 6;
    public static int brain_print_size = 100; // largest size of brain to print to System.out

    public static String inappropriate_aiml_file = "inappropriate.aiml";
    public static String profanity_aiml_file = "profanity.aiml";
    public static String insult_aiml_file = "insults.aiml";
    public static String reductions_update_aiml_file = "reductions_update.aiml";
    public static String deleted_aiml_file = "deleted.aiml";
    public static String predicates_aiml_file = "client_profile.aiml";
    public static String oob_aiml_file = "oob.aiml";
    public static String sraix_aiml_file = "sraix.aiml";
    public static String update_aiml_file = "update.aiml";
    public static String personality_aiml_file = "personality.aiml";
    // filter responses
    public static String inappropriate_filter = "FILTER INAPPROPRIATE";
    public static String profanity_filter = "FILTER PROFANITY";
    public static String insult_filter = "FILTER INSULT";
    // default templates
    public static String deleted_template = "deleted";
    public static String unfinished_template = "unfinished";

    private static final Logger log = LoggerFactory.getLogger(AB.class);

    private final boolean shuffleMode = true;

    private boolean sortMode = !this.shuffleMode;

    private final boolean filterAtomicMode = false;

    private final boolean filterWildMode = false;

    private final boolean offerAliceResponses = true;

    private final String logfile; //normal.txt";

    private int runCompletedCnt;

    public Bot bot;

    public Bot alice;

    private final AIMLSet passed;

    private final AIMLSet testSet;

    private final Graphmaster inputGraph;

    private final Graphmaster patternGraph;

    private final Graphmaster deletedGraph;

    private ArrayList<Category> suggestedCategories;

    public static int limit = 500000;

    private static int leafPatternCnt = 0;
    private static int starPatternCnt = 0;

    public AB(final Bot bot, final String sampleFile) {
        this.logfile = bot.getRootPath() + "/data/" + sampleFile;
        log.info("AB with sample file {}", this.logfile);
        this.bot = bot;
        this.inputGraph = new Graphmaster(bot, "input");
        this.deletedGraph = new Graphmaster(bot, "deleted");
        this.patternGraph = new Graphmaster(bot, "pattern");
        for (final Category c : bot.getBrain().getCategories()) {
            this.patternGraph.addCategory(c);
        }
        this.suggestedCategories = new ArrayList<>();
        this.passed = new AIMLSet("passed", bot);
        this.testSet = new AIMLSet("1000", bot);
        this.readDeletedIFCategories();
    }

    /**
     * Calculates the botmaster's productivity rate in
     * categories/sec when using Pattern Suggestor to create content.
     *
     * @param runCompletedCnt number of categories completed in this run
     * @param timer           tells elapsed time in ms
     * @see AB
     */
    private void productivity(final int runCompletedCnt, final Timer timer) {
        final float time = timer.elapsedTimeMins();
        log.info("Completed {} in {} min. Productivity {} cat/min", runCompletedCnt, time, runCompletedCnt / time);
    }

    private void readDeletedIFCategories() {
        this.bot.readCertainIFCategories(this.deletedGraph, deleted_aiml_file);
        if (log.isTraceEnabled()) {
            log.trace("--- DELETED CATEGORIES -- read {} deleted categories", this.deletedGraph.getCategories().size());
        }
    }

    private void writeDeletedIFCategories() {
        log.info("--- DELETED CATEGORIES -- write");
        this.bot.writeCertainIFCategories(this.deletedGraph, deleted_aiml_file);
        log.info("--- DELETED CATEGORIES -- write {} deleted categories", this.deletedGraph.getCategories().size());
    }

    /**
     * saves a new AIML category and increments runCompletedCnt
     *
     * @param pattern  the category's pattern (that and topic = *)
     * @param template the category's template
     * @param filename the filename for the category.
     */
    private void saveCategory(final String pattern, final String template, final String filename) {
        final String that = "*";
        final String topic = "*";
        final Category c = new Category(this.bot, 0, pattern, that, topic, template, filename);

        if (c.validate()) {
            this.bot.getBrain().addCategory(c);
            // bot.categories.add(c);
            this.bot.writeAIMLIFFiles();
            this.runCompletedCnt++;
        } else {
            log.warn("Invalid Category {}", c.getValidationMessage());
        }
    }

    /**
     * mark a category as deleted
     *
     * @param c the category
     */
    private void deleteCategory(final Category c) {
        c.setFilename(deleted_aiml_file);
        c.setTemplate(deleted_template);
        this.deletedGraph.addCategory(c);
        this.writeDeletedIFCategories();
    }

    /**
     * skip a category.  Make the category as "unfinished"
     *
     * @param c the category
     */
    private void skipCategory(final Category c) {
       /* bot.unfinishedGraph.addCategory(c);
        log.warn("{} unfinished categories", bot.unfinishedGraph.getCategories().size());
        bot.writeUnfinishedIFCategories();*/
    }

    public void abwq() {
        final Timer timer = new Timer();
        timer.start();
        this.classifyInputs(this.logfile);
        log.info("{} classifying inputs", timer.elapsedTimeSecs());
        this.bot.writeQuit();
    }

    /**
     * read sample inputs from filename, turn them into Paths, and
     * add them to the graph.
     *
     * @param filename file containing sample inputs
     */
    private void graphInputs(final String filename) {
        int count = 0;
        try (FileInputStream stream = new FileInputStream(filename)) {
            // Open the file that is the first
            // command line parameter
            // Get the object
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
                String strLine;
                //Read File Line By Line
                while ((strLine = reader.readLine()) != null && count < limit) {
                    //strLine = preProcessor.normalize(strLine);
                    final Category c = new Category(this.bot, 0, strLine, "*", "*", "nothing", Constants.unknownAimlFile);
                    final Nodemapper node = this.inputGraph.findNode(c);
                    if (node == null) {
                        this.inputGraph.addCategory(c);
                        c.incrementActivationCnt();
                    } else {
                        node.getCategory().incrementActivationCnt();
                    }
                    count++;
                }
            }
        } catch (final Exception e) {
            log.error("Error", e);
        }
    }

    /**
     * find suggested patterns in a graph of inputs
     */
    private void findPatterns() {
        this.findPatterns(this.inputGraph.getRoot(), "");
        log.info("{} Leaf Patterns, {} Star Patterns", leafPatternCnt, starPatternCnt);
    }

    /**
     * find patterns recursively
     *
     * @param node                    current graph node
     * @param partialPatternThatTopic partial pattern path
     */
    private void findPatterns(final Nodemapper node, final String partialPatternThatTopic) {
        if (NodemapperOperator.isLeaf(node)) {
            if (log.isTraceEnabled()) {
                log.trace("LEAF: {}. {}", node.getCategory().getActivationCnt(), partialPatternThatTopic);
            }
            if (node.getCategory().getActivationCnt() > node_activation_cnt) {
                if (log.isTraceEnabled()) {
                    log.trace("LEAF: {}. {} {}", node.getCategory().getActivationCnt(), partialPatternThatTopic, node.isShortCut());
                }
                leafPatternCnt++;
                try {
                    String categoryPatternThatTopic;
                    if (node.isShortCut()) {
                        categoryPatternThatTopic = partialPatternThatTopic + " <THAT> * <TOPIC> *";
                    } else {
                        categoryPatternThatTopic = partialPatternThatTopic;
                    }
                    final Category c = new Category(this.bot, 0, categoryPatternThatTopic, Constants.blank_template, Constants.unknownAimlFile);
                    if (!this.bot.getBrain().existsCategory(c) && !this.deletedGraph.existsCategory(c)) {
                        this.patternGraph.addCategory(c);
                        this.suggestedCategories.add(c);
                    }
                } catch (final Exception e) {
                    log.error("Error", e);
                }
            }
        }
        if (NodemapperOperator.size(node) > node_size) {
            if (log.isTraceEnabled()) {
                log.trace("STAR: {}. {} * <that> * <topic> *", NodemapperOperator.size(node), partialPatternThatTopic);
            }
            starPatternCnt++;
            try {
                final Category c = new Category(this.bot, 0, partialPatternThatTopic + " * <THAT> * <TOPIC> *", Constants.blank_template, Constants.unknownAimlFile);
                if (!this.bot.getBrain().existsCategory(c) && !this.deletedGraph.existsCategory(c)/* && !unfinishedGraph.existsCategory(c)*/) {
                    this.patternGraph.addCategory(c);
                    this.suggestedCategories.add(c);
                }
            } catch (final Exception e) {
                log.error("Error", e);
            }
        }
        for (final String key : NodemapperOperator.keySet(node)) {
            final Nodemapper value = NodemapperOperator.get(node, key);
            this.findPatterns(value, partialPatternThatTopic + " " + key);
        }
    }

    /**
     * classify inputs into matching categories
     *
     * @param filename file containing sample normalized inputs
     */

    private void classifyInputs(final String filename) {
        try (FileInputStream fstream = new FileInputStream(filename)) {
            // Get the object
            try (BufferedReader br = new BufferedReader(new InputStreamReader(fstream))) {
                String strLine;
                //Read File Line By Line
                int count = 0;
                while ((strLine = br.readLine()) != null && count < limit) {
                    if (log.isTraceEnabled()) {
                        log.trace("Classifying ", strLine);
                    }
                    if (strLine.startsWith("Human: ")) {
                        strLine = strLine.substring("Human: ".length(), strLine.length());
                    }
                    final String sentences[] = this.bot.getPreProcessor().sentenceSplit(strLine);
                    for (final String sentence : sentences) {
                        if (sentence.length() > 0) {
                            final Nodemapper match = this.patternGraph.match(sentence, "unknown", "unknown");

                            if (match == null) {
                                log.info("{} null match", sentence);
                            } else {
                                match.getCategory().incrementActivationCnt();
                                if (log.isDebugEnabled()) {
                                    log.debug("{}. {} matched {}", count, sentence, match.getCategory().inputThatTopic());
                                }
                            }
                            count += 1;
                            if (count % 10000 == 0) {
                                log.info("{}", count);
                            }
                        }
                    }
                }

                log.info("Finished classifying {} inputs", count);
            }
        } catch (final Exception e) {
            log.error("Error: ", e);
        }
    }

    /**
     * magically suggests new patterns for a bot.
     * Reads an input file of sample data called logFile.
     * Builds a graph of all the inputs.
     * Finds new patterns in the graph that are not already in the bot.
     * Classifies input log into those new patterns.
     */
    public void ab() {
        final String logFile = this.logfile;

        if (this.offerAliceResponses) {
            this.alice = new Bot(BotConfiguration.builder().name("alice").enableExternalSets(false).build());
        }
        final Timer timer = new Timer();
        this.bot.getBrain().nodeStats();
        if (this.bot.getBrain().getCategories().size() < brain_print_size) {
            this.bot.getBrain().printGraph();
        }
        timer.start();

        log.info("Graphing inputs");
        this.graphInputs(logFile);

        log.info("{} seconds Graphing inputs", timer.elapsedTimeSecs());
        this.inputGraph.nodeStats();
        if (this.inputGraph.getCategories().size() < brain_print_size) {
            this.inputGraph.printGraph();
        }

        timer.start();
        log.info("Finding Patterns");
        this.findPatterns();
        log.info("{} suggested categories, {} seconds finding patterns", this.suggestedCategories.size(), timer.elapsedTimeSecs());

        timer.start();
        this.patternGraph.nodeStats();
        if (this.patternGraph.getCategories().size() < brain_print_size) {
            this.patternGraph.printGraph();
        }
        log.info("Classifying Inputs from {}", logFile);
        this.classifyInputs(logFile);
        log.info("{} classifying inputs", timer.elapsedTimeSecs());
    }

    private ArrayList<Category> nonZeroActivationCount(final ArrayList<Category> suggestedCategories) {
        final ArrayList<Category> result = new ArrayList<>();
        for (final Category c : suggestedCategories) {
            if (c.getActivationCnt() > 0) {
                result.add(c);
            } else if (log.isDebugEnabled()) {
                log.info("[{}] {}", c.getActivationCnt(), c.inputThatTopic());
            }
        }
        return result;
    }

    /**
     * train the bot through a terminal interaction
     */
    public void terminalInteraction() {
        this.sortMode = !this.shuffleMode;
        if (this.sortMode) {
            this.suggestedCategories.sort(Category.ACTIVATION_COMPARATOR);
        }
        final ArrayList<Category> topSuggestCategories = new ArrayList<>();
        for (int i = 0; i < 10000 && i < this.suggestedCategories.size(); i++) {
            topSuggestCategories.add(this.suggestedCategories.get(i));
        }
        this.suggestedCategories = topSuggestCategories;
        if (this.shuffleMode) {
            Collections.shuffle(this.suggestedCategories);
        }

        final Timer timer = new Timer();
        timer.start();
        this.runCompletedCnt = 0;
        final ArrayList<Category> filteredAtomicCategories = new ArrayList<>();
        final ArrayList<Category> filteredWildCategories = new ArrayList<>();
        for (final Category c : this.suggestedCategories) {
            if (!c.getPattern().contains("*")) {
                filteredAtomicCategories.add(c);
            } else {
                filteredWildCategories.add(c);
            }
        }

        ArrayList<Category> browserCategories;
        if (this.filterAtomicMode) {
            browserCategories = filteredAtomicCategories;
        } else if (this.filterWildMode) {
            browserCategories = filteredWildCategories;
        } else {
            browserCategories = this.suggestedCategories;
        }
        if (log.isDebugEnabled()) {
            log.debug("{} filtered suggested categories", filteredAtomicCategories.size());
        }
        browserCategories = this.nonZeroActivationCount(browserCategories);

        boolean firstInteraction = true;
        String aliceTemplate = null;
        for (final Category c : browserCategories) {
            try {
                final List<String> samples = new ArrayList<>(c.getMatches(this.bot));
                Collections.shuffle(samples);
                final int sampleSize = Math.min(displayed_input_sample_size, c.getMatches(this.bot).size());
                for (int i = 0; i < sampleSize; i++) {
                    log.info("{}", samples.get(i));
                }

                log.info("[{}] {}", c.getActivationCnt(), c.inputThatTopic());
                Nodemapper node;
                if (this.offerAliceResponses) {
                    node = this.alice.getBrain().findNode(c);
                    if (node != null) {
                        aliceTemplate = node.getCategory().getTemplate();
                        String displayAliceTemplate = aliceTemplate;
                        displayAliceTemplate = displayAliceTemplate.replace("\n", " ");
                        if (displayAliceTemplate.length() > 200) {
                            displayAliceTemplate = displayAliceTemplate.substring(0, 200);
                        }
                        log.info("ALICE: {}", displayAliceTemplate);
                    } else {
                        aliceTemplate = null;
                    }
                }

                if (firstInteraction) {
                    timer.start();
                    firstInteraction = false;
                }
                this.productivity(this.runCompletedCnt, timer);
                this.terminalInteractionStep(this.bot, "", IOUtils.readInputTextLine(), c, aliceTemplate);
            } catch (final Exception e) {
                log.info("Returning to Category Browser", e);
            }
        }
        log.info("No more samples");
        this.bot.writeAIMLFiles();
        this.bot.writeAIMLIFFiles();
    }

    /**
     * process one step of the terminal interaction
     *
     * @param bot      the bot being trained.
     * @param request  used when this routine is called by benchmark testSuite
     * @param textLine response typed by the botmaster
     * @param c        AIML category selected
     */
    private void terminalInteractionStep(final Bot bot, final String request, String textLine, final Category c, final String alicetemplate) {
        String template;
        if (textLine.contains("<pattern>") && textLine.contains("</pattern>")) {
            final int index = textLine.indexOf("<pattern>") + "<pattern>".length();
            final int jndex = textLine.indexOf("</pattern>");
            final int kndex = jndex + "</pattern>".length();
            if (index < jndex) {
                final String pattern = textLine.substring(index, jndex);
                c.setPattern(pattern);
                textLine = textLine.substring(kndex, textLine.length());
                log.info("Got pattern = {} template = {}", pattern, textLine);
            }
        }
        String botThinks = "";
        final String[] pronouns = {"he", "she", "it", "we", "they"};
        for (final String p : pronouns) {
            if (textLine.contains("<" + p + ">")) {
                textLine = textLine.replace("<" + p + ">", "");
                botThinks = "<think><set name=\"" + p + "\"><set name=\"topic\"><star/></set></set></think>";
            }
        }
        if (textLine.equals("q")) {
            System.exit(0);       // Quit program
        } else if (textLine.equals("wq")) {   // Write AIML Files and quit program
            bot.writeQuit();
            System.exit(0);
        } else if (textLine.equals("skip") || textLine.equals("")) { // skip this one for now
            this.skipCategory(c);
        } else if (textLine.equals("s") || textLine.equals("pass")) { //
            this.passed.add(request);
            final AIMLSet difference = new AIMLSet("difference", bot);
            difference.addAll(this.testSet);
            difference.removeAll(this.passed);
            bot.getSets().write(difference);
            bot.getSets().write(this.passed);
        } else if (textLine.equals("a")) {
            template = alicetemplate;
            String filename;
            if (template.contains("<sr")) {
                filename = reductions_update_aiml_file;
            } else {
                filename = personality_aiml_file;
            }
            this.saveCategory(c.getPattern(), template, filename);
        } else if (textLine.equals("d")) { // delete this suggested category
            this.deleteCategory(c);
        } else if (textLine.equals("x")) {    // ask another bot
            template = "<sraix services=\"pannous\">" + c.getPattern().replace("*", "<star/>") + "</sraix>";
            template += botThinks;
            this.saveCategory(c.getPattern(), template, sraix_aiml_file);
        } else if (textLine.equals("p")) {   // filter inappropriate content
            template = "<srai>" + inappropriate_filter + "</srai>";
            template += botThinks;
            this.saveCategory(c.getPattern(), template, inappropriate_aiml_file);
        } else if (textLine.equals("f")) { // filter profanity
            template = "<srai>" + profanity_filter + "</srai>";
            template += botThinks;
            this.saveCategory(c.getPattern(), template, profanity_aiml_file);
        } else if (textLine.equals("i")) {
            template = "<srai>" + insult_filter + "</srai>";
            template += botThinks;
            this.saveCategory(c.getPattern(), template, insult_aiml_file);
        } else if (textLine.contains("<srai>") || textLine.contains("<sr/>")) {
            template = textLine;
            template += botThinks;
            this.saveCategory(c.getPattern(), template, reductions_update_aiml_file);
        } else if (textLine.contains("<oob>")) {
            template = textLine;
            template += botThinks;
            this.saveCategory(c.getPattern(), template, oob_aiml_file);
        } else if (textLine.contains("<set name") || botThinks.length() > 0) {
            template = textLine;
            template += botThinks;
            this.saveCategory(c.getPattern(), template, predicates_aiml_file);
        } else if (textLine.contains("<get name") && !textLine.contains("<get name=\"name")) {
            template = textLine;
            template += botThinks;
            this.saveCategory(c.getPattern(), template, predicates_aiml_file);
        } else {
            template = textLine;
            template += botThinks;
            this.saveCategory(c.getPattern(), template, personality_aiml_file);
        }
    }
}

