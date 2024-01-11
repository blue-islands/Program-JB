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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.goldrenard.jb.configuration.Constants;
import org.goldrenard.jb.model.AIMLSet;
import org.goldrenard.jb.model.Category;
import org.goldrenard.jb.model.Nodemapper;
import org.goldrenard.jb.model.Path;
import org.goldrenard.jb.model.StarBindings;
import org.goldrenard.jb.utils.Utilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Getter;
import lombok.Setter;

/**
 * The AIML Pattern matching algorithm and data structure.
 */
@Getter
@Setter
public class Graphmaster {

    private static final Logger log = LoggerFactory.getLogger(Graphmaster.class);

    private final static String botPropRegex = "<bot name=\"(.*?)\"/>";
    private final static Pattern botPropPattern = Pattern.compile(botPropRegex, Pattern.CASE_INSENSITIVE);

    private Bot bot;
    private String name;
    private final Nodemapper root;
    private int matchCount = 0;
    private int upgradeCnt = 0;
    private Set<String> vocabulary;
    private String resultNote = "";
    private int categoryCnt = 0;

    private int leafCnt;
    private int nodeCnt;
    private long nodeSize;
    private int singletonCnt;
    private int shortCutCnt;
    private int naryCnt;

    /**
     * Constructor
     *
     * @param bot the bot the graph belongs to.
     */
    public Graphmaster(final Bot bot) {
        this(bot, "brain");
    }

    public Graphmaster(final Bot bot, final String name) {
        this.root = new Nodemapper(bot.getConfiguration().getMaxGraphHeight());
        this.bot = bot;
        this.name = name;
        this.vocabulary = new HashSet<>();
    }

    /**
     * Convert input, that and topic to a single sentence having the form
     * {@code input <THAT> that <TOPIC> topic}
     *
     * @param input input (or input pattern)
     * @param that  that (or that pattern)
     * @param topic topic (or topic pattern)
     * @return {@code input <THAT> that <TOPIC> topic}
     */
    public static String inputThatTopic(final String input, final String that, final String topic) {
        return input.trim() + " <THAT> " + that.trim() + " <TOPIC> " + topic.trim();
    }

    public String replaceBotProperties(String pattern) {
        if (pattern.contains("<B")) {
            final Matcher matcher = botPropPattern.matcher(pattern);
            while (matcher.find()) {
                final String propName = matcher.group(1).toLowerCase();
                final String property = this.bot.getProperties().get(propName).toUpperCase();
                pattern = pattern.replaceFirst("(?i)" + botPropRegex, property);
            }
        }
        return pattern;
    }

    /**
     * add an AIML category to this graph.
     *
     * @param category AIML Category
     */
    public void addCategory(final Category category) {
        String inputThatTopic = inputThatTopic(category.getPattern(), category.getThat(), category.getTopic());
        inputThatTopic = this.replaceBotProperties(inputThatTopic);
        final Path p = Path.sentenceToPath(inputThatTopic);
        this.addPath(p, category);
        this.categoryCnt++;
    }

    private boolean thatStarTopicStar(final Path path) {
        final String tail = Path.pathToSentence(path).trim();
        return tail.equals("<THAT> * <TOPIC> *");
    }

    private void addSets(final String type, final Bot bot, final Nodemapper node, final String filename) {
        final String setName = Utilities.tagTrim(type, "SET").toLowerCase();
        if (bot.getSets().containsKey(setName)) {
            if (node.getSets() == null) {
                node.setSets(new ArrayList<>());
            }
            if (!node.getSets().contains(setName)) {
                node.getSets().add(setName);
            }
        } else {
            log.warn("No AIML Set found for <set>{}</set> in {} {}", setName, bot.getName(), filename);
        }
    }

    /**
     * add a path to the graph from the root to a Category
     *
     * @param path     Pattern path
     * @param category AIML category
     */
    private void addPath(final Path path, final Category category) {
        this.addPath(this.root, path, category);
    }

    /**
     * add a Path to the graph from a given node.
     * Shortcuts: Replace all instances of paths "<THAT> * <TOPIC> *" with a direct link to the matching category
     *
     * @param node     starting node in graph
     * @param path     Pattern path to be added
     * @param category AIML Category
     */
    private void addPath(final Nodemapper node, final Path path, final Category category) {
        if (path == null) {
            node.setCategory(category);
            node.setHeight(0);
        } else if (this.bot.getConfiguration().isGraphShortCuts() && this.thatStarTopicStar(path)) {
            node.setCategory(category);
            node.setHeight(Math.min(4, node.getHeight()));
            node.setShortCut(true);
        } else if (NodemapperOperator.containsKey(node, path.getWord())) {
            if (path.getWord().startsWith("<SET>")) {
                this.addSets(path.getWord(), this.bot, node, category.getFilename());
            }
            final Nodemapper nextNode = NodemapperOperator.get(node, path.getWord());
            this.addPath(nextNode, path.getNext(), category);
            int offset = 1;
            if (path.getWord().equals("#") || path.getWord().equals("^")) {
                offset = 0;
            }
            node.setHeight(Math.min(offset + (nextNode != null ? nextNode.getHeight() : 0), node.getHeight()));
        } else {
            final Nodemapper nextNode = new Nodemapper(this.bot.getConfiguration().getMaxGraphHeight());
            if (path.getWord().startsWith("<SET>")) {
                this.addSets(path.getWord(), this.bot, node, category.getFilename());
            }
            if (node.getKey() != null) {
                NodemapperOperator.upgrade(node);
                this.upgradeCnt++;
            }
            NodemapperOperator.put(node, path.getWord(), nextNode);
            this.addPath(nextNode, path.getNext(), category);
            int offset = 1;
            if (path.getWord().equals("#") || path.getWord().equals("^")) {
                offset = 0;
            }
            node.setHeight(Math.min(offset + nextNode.getHeight(), node.getHeight()));
        }
    }

    /**
     * test if category is already in graph
     *
     * @return true or false
     */
    public boolean existsCategory(final Category c) {
        return (this.findNode(c) != null);
    }

    /**
     * test if category is already in graph
     *
     * @return true or false
     */
    public Nodemapper findNode(final Category c) {
        return this.findNode(c.getPattern(), c.getThat(), c.getTopic());
    }

    /**
     * Given an input pattern, that pattern and topic pattern, find the leaf node associated with this path.
     *
     * @param input input pattern
     * @param that  that pattern
     * @param topic topic pattern
     * @return leaf node or null if no matching node is found
     */
    public Nodemapper findNode(final String input, final String that, final String topic) {
        final Nodemapper result = this.findNode(this.root, Path.sentenceToPath(inputThatTopic(input, that, topic)));
        if (log.isTraceEnabled()) {
            log.trace("findNode " + inputThatTopic(input, that, topic) + " " + result);
        }
        return result;
    }

    /**
     * Recursively find a leaf node given a starting node and a path.
     *
     * @param node string node
     * @param path string path
     * @return the leaf node or null if no leaf is found
     */
    private Nodemapper findNode(final Nodemapper node, final Path path) {
        if (path == null && node != null) {
            if (log.isTraceEnabled()) {
                log.trace("findNode: path is null, returning node {}", node.getCategory().inputThatTopic());
            }
            return node;
        } else if (node != null && Path.pathToSentence(path).trim().equals("<THAT> * <TOPIC> *") && node.isShortCut() && path.getWord().equals("<THAT>")) {
            if (log.isTraceEnabled()) {
                log.trace("findNode: shortcut, returning {}", node.getCategory().inputThatTopic());
            }
            return node;
        } else if (path != null && NodemapperOperator.containsKey(node, path.getWord())) {
            if (log.isTraceEnabled()) {
                log.trace("findNode: node contains {}", path.getWord());
            }
            final Nodemapper nextNode = NodemapperOperator.get(node, path.getWord().toUpperCase());
            return this.findNode(nextNode, path.getNext());
        }
        if (log.isTraceEnabled()) {
            log.trace("findNode: returning null");
        }
        return null;
    }

    /**
     * Find the matching leaf node given an input, that state and topic value
     *
     * @param input client input
     * @param that  bot's last sentence
     * @param topic current topic
     * @return matching leaf node or null if no match is found
     */
    public final Nodemapper match(final String input, final String that, final String topic) {
        Nodemapper n;
        try {
            final String inputThatTopic = inputThatTopic(input, that, topic);
            final Path p = Path.sentenceToPath(inputThatTopic);
            n = this.match(p, inputThatTopic);

            if (log.isTraceEnabled()) {
                if (n != null) {
                    log.trace("Matched: {} {}", n.getCategory().inputThatTopic(), n.getCategory().getFilename());
                } else {
                    log.trace("No match.");
                }
            }
        } catch (final Exception e) {
            log.error("Error: ", e);
            n = null;
        }
        return n;
    }

    /**
     * Find the matching leaf node given a path of the form "{@code input <THAT> that <TOPIC> topic}"
     *
     * @param path           Path
     * @param inputThatTopic InputTopic
     * @return matching leaf node or null if no match is found
     */
    private Nodemapper match(final Path path, final String inputThatTopic) {
        try {
            final int maxStars = this.bot.getConfiguration().getMaxStars();
            final String[] inputStars = new String[maxStars];
            final String[] thatStars = new String[maxStars];
            final String[] topicStars = new String[maxStars];
            final String starState = "inputStar";
            final String matchTrace = "";
            final Nodemapper n = this.match(path, this.root, inputThatTopic, starState, 0, inputStars, thatStars, topicStars, matchTrace);
            if (n != null) {
                final StarBindings sb = new StarBindings();
                for (int i = 0; inputStars[i] != null && i < maxStars; i++) {
                    sb.getInputStars().add(inputStars[i]);
                }
                for (int i = 0; thatStars[i] != null && i < maxStars; i++) {
                    sb.getThatStars().add(thatStars[i]);
                }
                for (int i = 0; topicStars[i] != null && i < maxStars; i++) {
                    sb.getTopicStars().add(topicStars[i]);
                }
                n.setStarBindings(sb);
            }
            if (n != null) {
                n.getCategory().addMatch(inputThatTopic, this.bot);
            }
            return n;
        } catch (final Exception e) {
            log.error("Error: ", e);
        }
        return null;
    }

    /**
     * Depth-first search of the graph for a matching leaf node.
     * At each node, the order of search is
     * 1. $WORD  (high priority exact word match)
     * 2. # wildcard  (zero or more word match)
     * 3. _ wildcard (one or more words match)
     * 4. WORD (exact word match)
     * 5. {@code <set></set>} (AIML Set match)
     * 6. shortcut (graph shortcut when that pattern = * and topic pattern = *)
     * 7. ^ wildcard  (zero or more words match)
     * 8. * wildcard (one or more words match)
     *
     * @param path           remaining path to be matched
     * @param node           current search node
     * @param inputThatTopic original input, that and topic string
     * @param starState      tells whether wildcards are in input pattern, that pattern or topic pattern
     * @param starIndex      index of wildcard
     * @param inputStars     array of input pattern wildcard matches
     * @param thatStars      array of that pattern wildcard matches
     * @param topicStars     array of topic pattern wildcard matches
     * @param matchTrace     trace of match path for debugging purposes
     * @return matching leaf node or null if no match is found
     */
    private Nodemapper match(final Path path, final Nodemapper node, final String inputThatTopic, final String starState, final int starIndex,
                             final String[] inputStars, final String[] thatStars, final String[] topicStars, final String matchTrace) {
        Nodemapper matchedNode;
        this.matchCount++;
        if ((matchedNode = this.nullMatch(path, node, matchTrace)) != null) {
            return matchedNode;
        } else if (path.getLength() < node.getHeight()) {
            return null;
        } else if ((matchedNode = this.dollarMatch(path, node, inputThatTopic, starState, starIndex, inputStars,
                thatStars, topicStars, matchTrace)) != null) {
            return matchedNode;
        } else if ((matchedNode = this.sharpMatch(path, node, inputThatTopic, starState, starIndex, inputStars,
                thatStars, topicStars, matchTrace)) != null) {
            return matchedNode;
        } else if ((matchedNode = this.underMatch(path, node, inputThatTopic, starState, starIndex, inputStars,
                thatStars, topicStars, matchTrace)) != null) {
            return matchedNode;
        } else if ((matchedNode = this.wordMatch(path, node, inputThatTopic, starState, starIndex, inputStars,
                thatStars, topicStars, matchTrace)) != null) {
            return matchedNode;
        } else if ((matchedNode = this.setMatch(path, node, inputThatTopic, starState, starIndex, inputStars,
                thatStars, topicStars, matchTrace)) != null) {
            return matchedNode;
        } else if ((matchedNode = this.shortCutMatch(path, node, inputThatTopic, starState, starIndex, inputStars,
                thatStars, topicStars, matchTrace)) != null) {
            return matchedNode;
        } else if ((matchedNode = this.caretMatch(path, node, inputThatTopic, starState, starIndex, inputStars,
                thatStars, topicStars, matchTrace)) != null) {
            return matchedNode;
        } else if ((matchedNode = this.starMatch(path, node, inputThatTopic, starState, starIndex, inputStars,
                thatStars, topicStars, matchTrace)) != null) {
            return matchedNode;
        }
        return null;
    }

    /**
     * print out match trace when search fails
     *
     * @param mode  Which mode of search
     * @param trace Match trace info
     */
    private void fail(final String mode, final String trace) {
        if (log.isTraceEnabled()) {
            log.trace("Match failed ({}) {}", mode, trace);
        }
    }

    /**
     * a match is found if the end of the path is reached and the node is a leaf node
     *
     * @param path       remaining path
     * @param node       current search node
     * @param matchTrace trace of match for debugging purposes
     * @return matching leaf node or null if no match found
     */
    private Nodemapper nullMatch(final Path path, final Nodemapper node, final String matchTrace) {
        if (path == null && node != null && NodemapperOperator.isLeaf(node) && node.getCategory() != null) {
            return node;
        }
        this.fail("null", matchTrace);
        return null;
    }

    private Nodemapper shortCutMatch(final Path path, final Nodemapper node, final String inputThatTopic, final String starState,
                                     final int starIndex, final String[] inputStars, final String[] thatStars, final String[] topicStars,
                                     final String matchTrace) {
        if (node != null && node.isShortCut() && path.getWord().equals("<THAT>") && node.getCategory() != null) {
            final String tail = Path.pathToSentence(path).trim();
            final String that = tail.substring(tail.indexOf("<THAT>") + "<THAT>".length(), tail.indexOf("<TOPIC>")).trim();
            final String topic = tail.substring(tail.indexOf("<TOPIC>") + "<TOPIC>".length(), tail.length()).trim();
            thatStars[0] = that;
            topicStars[0] = topic;
            return node;
        }
        this.fail("shortCut", matchTrace);
        return null;
    }

    private Nodemapper wordMatch(final Path path, final Nodemapper node, final String inputThatTopic, String starState, int starIndex,
                                 final String[] inputStars, final String[] thatStars, final String[] topicStars, String matchTrace) {
        Nodemapper matchedNode;
        try {
            final String uword = path.getWord().toUpperCase();
            if (uword.equals("<THAT>")) {
                starIndex = 0;
                starState = "thatStar";
            } else if (uword.equals("<TOPIC>")) {
                starIndex = 0;
                starState = "topicStar";
            }
            matchTrace += "[" + uword + "," + uword + "]";
            if (NodemapperOperator.containsKey(node, uword) &&
                    (matchedNode = this.match(path.getNext(), NodemapperOperator.get(node, uword), inputThatTopic, starState,
                            starIndex, inputStars, thatStars, topicStars, matchTrace)) != null) {
                return matchedNode;
            }
            this.fail("word", matchTrace);
            return null;
        } catch (final Exception e) {
            log.error("Error: ", e);
        }
        return null;
    }

    private Nodemapper dollarMatch(final Path path, final Nodemapper node, final String inputThatTopic, final String starState, final int starIndex,
                                   final String[] inputStars, final String[] thatStars, final String[] topicStars, final String matchTrace) {
        final String uword = "$" + path.getWord().toUpperCase();
        Nodemapper matchedNode;
        if (NodemapperOperator.containsKey(node, uword) && (matchedNode = this.match(path.getNext(),
                NodemapperOperator.get(node, uword), inputThatTopic, starState, starIndex,
                inputStars, thatStars, topicStars, matchTrace)) != null) {
            return matchedNode;
        }
        this.fail("dollar", matchTrace);
        return null;
    }

    private Nodemapper starMatch(final Path path, final Nodemapper node, final String input, final String starState, final int starIndex,
                                 final String[] inputStars, final String[] thatStars, final String[] topicStars, final String matchTrace) {
        return this.wildMatch(path, node, input, starState, starIndex, inputStars, thatStars, topicStars, "*",
                matchTrace);
    }

    private Nodemapper underMatch(final Path path, final Nodemapper node, final String input, final String starState, final int starIndex,
                                  final String[] inputStars, final String[] thatStars, final String[] topicStars, final String matchTrace) {
        return this.wildMatch(path, node, input, starState, starIndex, inputStars, thatStars, topicStars, "_",
                matchTrace);
    }

    private Nodemapper caretMatch(final Path path, final Nodemapper node, final String input, final String starState, final int starIndex,
                                  final String[] inputStars, final String[] thatStars, final String[] topicStars, final String matchTrace) {
        Nodemapper matchedNode;
        matchedNode = this.zeroMatch(path, node, input, starState, starIndex, inputStars, thatStars, topicStars, "^",
                matchTrace);
        if (matchedNode != null) {
            return matchedNode;
        }
        return this.wildMatch(path, node, input, starState, starIndex, inputStars, thatStars, topicStars, "^",
                matchTrace);
    }

    private Nodemapper sharpMatch(final Path path, final Nodemapper node, final String input, final String starState, final int starIndex,
                                  final String[] inputStars, final String[] thatStars, final String[] topicStars, final String matchTrace) {
        Nodemapper matchedNode;
        matchedNode = this.zeroMatch(path, node, input, starState, starIndex, inputStars, thatStars, topicStars, "#",
                matchTrace);
        if (matchedNode != null) {
            return matchedNode;
        }
        return this.wildMatch(path, node, input, starState, starIndex, inputStars, thatStars, topicStars, "#",
                matchTrace);
    }

    private Nodemapper zeroMatch(final Path path, final Nodemapper node, final String input, final String starState, final int starIndex,
                                 final String[] inputStars, final String[] thatStars, final String[] topicStars, final String wildcard,
                                 String matchTrace) {
        matchTrace += "[" + wildcard + ",]";
        if (path != null && NodemapperOperator.containsKey(node, wildcard)) {
            this.setStars(this.bot.getProperties().get(Constants.null_star), starIndex, starState, inputStars, thatStars, topicStars);
            final Nodemapper nextNode = NodemapperOperator.get(node, wildcard);
            return this.match(path, nextNode, input, starState, starIndex + 1, inputStars, thatStars, topicStars, matchTrace);
        }
        this.fail("zero " + wildcard, matchTrace);
        return null;
    }

    private Nodemapper wildMatch(Path path, final Nodemapper node, final String input, final String starState, final int starIndex,
                                 final String[] inputStars, final String[] thatStars, final String[] topicStars, final String wildcard,
                                 String matchTrace) {
        Nodemapper matchedNode;
        if ("<THAT>".equals(path.getWord()) || "<TOPIC>".equals(path.getWord())) {
            this.fail("wild1 " + wildcard, matchTrace);
            return null;
        }
        try {
            if (NodemapperOperator.containsKey(node, wildcard)) {
                matchTrace += "[" + wildcard + "," + path.getWord() + "]";
                final Nodemapper nextNode = NodemapperOperator.get(node, wildcard);
                if (nextNode != null) {
                    String currentWord = path.getWord();
                    String starWords = currentWord + " ";
                    final Path pathStart = path.getNext();
                    if (NodemapperOperator.isLeaf(nextNode) && !nextNode.isShortCut()) {
                        matchedNode = nextNode;
                        starWords = Path.pathToSentence(path);
                        this.setStars(starWords, starIndex, starState, inputStars, thatStars, topicStars);
                        return matchedNode;
                    } else {
                        for (path = pathStart; path != null && !currentWord.equals("<THAT>") && !currentWord.equals("<TOPIC>"); path = path.getNext()) {
                            matchTrace += "[" + wildcard + "," + path.getWord() + "]";
                            if ((matchedNode = this.match(path, nextNode, input, starState, starIndex + 1,
                                    inputStars, thatStars, topicStars, matchTrace)) != null) {
                                this.setStars(starWords, starIndex, starState, inputStars, thatStars, topicStars);
                                return matchedNode;
                            } else {
                                currentWord = path.getWord();
                                starWords += currentWord + " ";
                            }
                        }
                        this.fail("wild2 " + wildcard, matchTrace);
                        return null;
                    }
                }
            }
        } catch (final Exception e) {
            log.error("Error: ", e);
        }
        this.fail("wild3 " + wildcard, matchTrace);
        return null;
    }

    private Nodemapper setMatch(final Path path, final Nodemapper node, final String input, final String starState, final int starIndex,
                                final String[] inputStars, final String[] thatStars, final String[] topicStars, String matchTrace) {
        if (log.isDebugEnabled()) {
            log.debug("Graphmaster.setMatch(path: {}, node: {}, input: {}, starState: {}, " +
                            "starIndex: {}, inputStars, thatStars, topicStars, matchTrace: {})",
                    path, node, input, starState, starIndex, matchTrace);
        }

        if (node.getSets() == null || path.getWord().equals("<THAT>") || path.getWord().equals("<TOPIC>")) {
            return null;
        }
        if (log.isDebugEnabled()) {
            log.debug("in Graphmaster.setMatch, setMatch sets = {}", node.getSets());
        }

        for (final String setName : node.getSets()) {
            if (log.isDebugEnabled()) {
                log.debug("in Graphmaster.setMatch, setMatch trying type {}", setName);
            }
            final Nodemapper nextNode = NodemapperOperator.get(node, "<SET>" + setName.toUpperCase() + "</SET>");
            final AIMLSet aimlSet = this.bot.getSets().get(setName);
            Nodemapper matchedNode;
            Nodemapper bestMatchedNode = null;
            String currentWord = path.getWord();
            String starWords = currentWord + " ";
            int length = 1;
            matchTrace += "[<set>" + setName + "</set>," + path.getWord() + "]";
            if (log.isDebugEnabled()) {
                log.debug("in Graphmaster.setMatch, setMatch starWords =\"{}\"", starWords);
            }
            for (Path qath = path.getNext(); qath != null && !currentWord.equals("<THAT>") && !currentWord.equals("<TOPIC>") && length <= aimlSet.getMaxLength(); qath = qath.getNext()) {
                if (log.isDebugEnabled()) {
                    log.debug("in Graphmaster.setMatch, qath.word = {}", qath.getWord());
                }
                final String phrase = this.bot.getPreProcessor().normalize(starWords.trim()).toUpperCase();
                if (log.isDebugEnabled()) {
                    log.debug("in Graphmaster.setMatch, setMatch trying \"{}\" in {}", phrase, setName);
                }
                if (aimlSet.contains(phrase) && (matchedNode = this.match(qath, nextNode, input, starState, starIndex + 1, inputStars, thatStars, topicStars, matchTrace)) != null) {
                    this.setStars(starWords, starIndex, starState, inputStars, thatStars, topicStars);
                    if (log.isDebugEnabled()) {
                        log.debug("in Graphmaster.setMatch, setMatch found {} in {}", phrase, setName);
                    }
                    bestMatchedNode = matchedNode;
                }
                length = length + 1;
                currentWord = qath.getWord();
                starWords += currentWord + " ";
            }
            if (bestMatchedNode != null) {
                return bestMatchedNode;
            }
        }
        this.fail("set", matchTrace);
        return null;
    }

    private void setStars(String starWords, final int starIndex, final String starState, final String[] inputStars, final String[] thatStars, final String[] topicStars) {
        if (starIndex < this.bot.getConfiguration().getMaxStars()) {
            starWords = starWords.trim();
            switch (starState) {
                case "inputStar":
                    inputStars[starIndex] = starWords;
                    break;
                case "thatStar":
                    thatStars[starIndex] = starWords;
                    break;
                case "topicStar":
                    topicStars[starIndex] = starWords;
                    break;
            }
        }
    }

    public void printGraph() {
        this.printGraph(this.root, "");
    }

    private void printGraph(final Nodemapper node, final String partial) {
        if (node == null) {
            log.info("Null graph");
        } else {
            if (NodemapperOperator.isLeaf(node) || node.isShortCut()) {
                String template = node.getCategory().getTemplateLine();
                template = template.substring(0, Math.min(16, template.length()));
                if (node.isShortCut()) {
                    log.info("{}({}[{}])--<THAT>-->X(1)--*-->X(1)--<TOPIC>-->X(1)--*-->{}...", partial,
                            NodemapperOperator.size(node), node.getHeight(), template);
                } else {
                    log.info("{}({}[{}]) {}...", partial, NodemapperOperator.size(node), node.getHeight(), template);
                }
            }
            for (final String key : NodemapperOperator.keySet(node)) {
                this.printGraph(NodemapperOperator.get(node, key), partial + "(" + NodemapperOperator.size(node) +
                        "[" + node.getHeight() + "])--" + key + "-->");
            }
        }
    }

    public ArrayList<Category> getCategories() {
        final ArrayList<Category> categories = new ArrayList<>();
        this.getCategories(this.root, categories);
        return categories;
    }

    private void getCategories(final Nodemapper node, final ArrayList<Category> categories) {
        if (node != null) {
            if (NodemapperOperator.isLeaf(node) || node.isShortCut()) {
                if (node.getCategory() != null) {
                    categories.add(node.getCategory());   // node.category == null when the category is deleted.
                }
            }
            for (final String key : NodemapperOperator.keySet(node)) {
                this.getCategories(NodemapperOperator.get(node, key), categories);
            }
        }
    }

    public void nodeStats() {
        this.leafCnt = 0;
        this.nodeCnt = 0;
        this.nodeSize = 0;
        this.singletonCnt = 0;
        this.shortCutCnt = 0;
        this.naryCnt = 0;
        this.nodeStatsGraph(this.root);
        this.resultNote = this.bot.getName() + " (" + this.name + "): " + this.getCategories().size() + " categories " + this.nodeCnt +
                " nodes " + this.singletonCnt + " singletons " + this.leafCnt + " leaves " + this.shortCutCnt + " shortcuts " +
                this.naryCnt + " n-ary " + this.nodeSize + " branches " + (float) this.nodeSize / (float) this.nodeCnt + " average branching ";
        if (log.isTraceEnabled()) {
            log.trace(this.resultNote);
        }
    }

    private void nodeStatsGraph(final Nodemapper node) {
        if (node != null) {
            this.nodeCnt++;
            this.nodeSize += NodemapperOperator.size(node);
            if (NodemapperOperator.size(node) == 1) {
                this.singletonCnt += 1;
            }
            if (NodemapperOperator.isLeaf(node) && !node.isShortCut()) {
                this.leafCnt++;
            }
            if (NodemapperOperator.size(node) > 1) {
                this.naryCnt += 1;
            }
            if (node.isShortCut()) {
                this.shortCutCnt += 1;
            }
            for (final String key : NodemapperOperator.keySet(node)) {
                this.nodeStatsGraph(NodemapperOperator.get(node, key));
            }
        }
    }

    public Set<String> getVocabulary() {
        this.vocabulary = new HashSet<>();
        this.getBrainVocabulary(this.root);
        for (final String set : this.bot.getSets().keySet()) {
            this.vocabulary.addAll(this.bot.getSets().get(set));
        }
        return this.vocabulary;
    }

    public void getBrainVocabulary(final Nodemapper node) {
        if (node != null) {
            for (final String key : NodemapperOperator.keySet(node)) {
                this.vocabulary.add(key);
                this.getBrainVocabulary(NodemapperOperator.get(node, key));
            }
        }
    }
}
