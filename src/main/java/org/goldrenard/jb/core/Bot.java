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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.goldrenard.jb.configuration.BotConfiguration;
import org.goldrenard.jb.configuration.Constants;
import org.goldrenard.jb.model.AIMLMap;
import org.goldrenard.jb.model.AIMLSet;
import org.goldrenard.jb.model.Category;
import org.goldrenard.jb.model.Nodemapper;
import org.goldrenard.jb.model.Properties;
import org.goldrenard.jb.parser.MapsResource;
import org.goldrenard.jb.parser.PronounsResource;
import org.goldrenard.jb.parser.SetsResource;
import org.goldrenard.jb.parser.base.CollectionResource;
import org.goldrenard.jb.parser.base.NamedResource;
import org.goldrenard.jb.utils.IOUtils;
import org.goldrenard.jb.utils.Timer;
import org.goldrenard.jb.utils.Utilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Getter;
import lombok.Setter;

/**
 * Class representing the AIML bot
 */
@Getter
@Setter
public class Bot {

    private static final Logger log = LoggerFactory.getLogger(Bot.class);

    private final BotConfiguration configuration;

    private final Properties properties = new Properties();

    private final PreProcessor preProcessor;

    private final AIMLProcessor processor;

    private final Graphmaster brain;

    private Graphmaster learnfGraph;

    private Graphmaster learnGraph;

    private String name;

    private NamedResource<AIMLSet> sets = new SetsResource(this);

    private NamedResource<AIMLMap> maps = new MapsResource(this);

    private CollectionResource<String> pronouns = new PronounsResource();

    private String rootPath;
    private String aimlifPath;
    private String aimlPath;
    private String configPath;
    private String setsPath;
    private String mapsPath;

    public Bot() {
        this(BotConfiguration.builder().build());
    }

    /**
     * Constructor (default action, default path)
     *
     * @param name Bot Name
     */
    public Bot(final String name) {
        this(BotConfiguration.builder().name(name).build());
    }

    /**
     * Constructor
     *
     * @param configuration configuration
     */
    public Bot(final BotConfiguration configuration) {
        this.configuration = configuration;
        this.name = configuration.getName();
        this.setAllPaths(configuration);
        this.brain = new Graphmaster(this);
        this.learnfGraph = new Graphmaster(this, "learnf");
        this.learnGraph = new Graphmaster(this, "learn");
        this.preProcessor = new PreProcessor(this);
        this.processor = new AIMLProcessor(this);
        this.addProperties();

        int count = this.sets.read(this.setsPath);
        if (log.isDebugEnabled()) {
            log.debug("Loaded {} set elements.", count);
        }

        count = this.maps.read(this.mapsPath);
        if (log.isDebugEnabled()) {
            log.debug("Loaded {} map elements.", count);
        }

        count = this.pronouns.read(this.configPath);
        if (log.isDebugEnabled()) {
            log.debug("Loaded {} pronouns.", count);
        }

        final Date aimlDate = new Date(new File(this.aimlPath).lastModified());
        final Date aimlIFDate = new Date(new File(this.aimlifPath).lastModified());
        if (log.isDebugEnabled()) {
            log.debug("AIML modified {} AIMLIF modified {}", aimlDate, aimlIFDate);
        }

        switch (configuration.getAction()) {
            case "aiml2csv":
                this.addCategoriesFromAIML();
                break;
            case "csv2aiml":
            case "chat-app":
                this.addCategoriesFromAIMLIF();
                break;
            default:
                if (aimlDate.after(aimlIFDate)) {
                    this.addCategoriesFromAIML();
                    this.writeAIMLIFFiles();
                } else {
                    this.addCategoriesFromAIMLIF();
                    if (this.brain.getCategories().size() == 0) {
                        this.addCategoriesFromAIML();
                    }
                }
                break;
        }

        final Category version = new Category(this, 0, "PROGRAM VERSION", "*", "*",
                configuration.getProgramName(), "update.aiml");
        this.brain.addCategory(version);
        this.brain.nodeStats();
        this.learnfGraph.nodeStats();
    }

    /**
     * Set all directory path variables for this bot
     *
     * @param configuration configuration of Program AB
     */
    private void setAllPaths(final BotConfiguration configuration) {
        this.rootPath = configuration.getPath();
        final String botNamePath = this.rootPath + "/bots/" + this.name;
        if (log.isTraceEnabled()) {
            log.trace("Init bot: Name = {} Path = {}", this.name, botNamePath);
        }
        this.aimlPath = botNamePath + "/aiml";
        this.aimlifPath = botNamePath + "/aimlif";
        this.configPath = botNamePath + "/config";
        this.setsPath = botNamePath + "/sets";
        this.mapsPath = botNamePath + "/maps";
    }

    /**
     * add an array list of categories with a specific file name
     *
     * @param file           name of AIML file
     * @param moreCategories list of categories
     */
    private void addMoreCategories(final String file, final ArrayList<Category> moreCategories) {
        if (file.contains(Constants.learnfAimlFile)) {
            for (final Category c : moreCategories) {
                this.brain.addCategory(c);
                this.learnfGraph.addCategory(c);
            }
        } else {
            for (final Category c : moreCategories) {
                this.brain.addCategory(c);
            }
        }
    }

    /**
     * Load all brain categories from AIML directory
     */
    private void addCategoriesFromAIML() {
        final Timer timer = new Timer();
        timer.start();
        int count = 0;
        try {
            // Directory path here
            String file;
            final File folder = new File(this.aimlPath);
            if (folder.exists()) {
                final File[] listOfFiles = IOUtils.listFiles(folder);
                if (log.isTraceEnabled()) {
                    log.trace("Loading AIML files from {}", this.aimlPath);
                }
                for (final File listOfFile : listOfFiles) {
                    if (listOfFile.isFile()) {
                        file = listOfFile.getName();
                        if (file.endsWith(".aiml") || file.endsWith(".AIML")) {
                            if (log.isTraceEnabled()) {
                                log.trace("Reading AIML {}", file);
                            }
                            try {
                                final ArrayList<Category> moreCategories = this.processor.AIMLToCategories(this.aimlPath, file);
                                this.addMoreCategories(file, moreCategories);
                                count += moreCategories != null ? moreCategories.size() : 0;
                            } catch (final Exception e) {
                                log.error("Problem loading {}", file, e);
                            }
                        }
                    }
                }
            } else {
                log.warn("addCategoriesFromAIML: {} does not exist.", this.aimlPath);
            }
        } catch (final Exception e) {
            log.error("Error: ", e);
        }
        if (log.isTraceEnabled()) {
            log.trace("Loaded {} categories in {} sec", count, timer.elapsedTimeSecs());
        }
    }

    /**
     * load all brain categories from AIMLIF directory
     */
    private void addCategoriesFromAIMLIF() {
        final Timer timer = new Timer();
        timer.start();
        int count = 0;
        try {
            // Directory path here
            String file;
            final File folder = new File(this.aimlifPath);
            if (folder.exists()) {
                final File[] listOfFiles = IOUtils.listFiles(folder);
                if (log.isTraceEnabled()) {
                    log.trace("Loading AIML files from {}", this.aimlifPath);
                }
                for (final File listOfFile : listOfFiles) {
                    if (listOfFile.isFile()) {
                        file = listOfFile.getName();
                        if (file.endsWith(this.configuration.getAimlifFileSuffix()) || file.endsWith(this.configuration.getAimlifFileSuffix().toUpperCase())) {
                            if (log.isTraceEnabled()) {
                                log.trace("Reading AIML {}", file);
                            }
                            try {
                                final ArrayList<Category> moreCategories = this.readIFCategories(this.aimlifPath + "/" + file);
                                count += moreCategories.size();
                                this.addMoreCategories(file, moreCategories);
                            } catch (final Exception e) {
                                log.error("Problem loading {}", file, e);
                            }
                        }
                    }
                }

            } else {
                log.warn("addCategoriesFromAIMLIF: {} does not exist.", this.aimlifPath);
            }
        } catch (final Exception e) {
            log.error("Error: ", e);
        }
        if (log.isTraceEnabled()) {
            log.trace("Loaded {} categories in {} sec", count, timer.elapsedTimeSecs());
        }
    }

    /**
     * write all AIML and AIMLIF categories
     */
    public void writeQuit() {
        this.writeAIMLIFFiles();
        this.writeAIMLFiles();
    }

    /**
     * read categories from specified AIMLIF file into specified graph
     *
     * @param graph    Graphmaster to store categories
     * @param fileName file name of AIMLIF file
     */
    public void readCertainIFCategories(final Graphmaster graph, final String fileName) {
        int count;
        final String filePath = this.aimlifPath + "/" + fileName + this.configuration.getAimlifFileSuffix();
        final File file = new File(filePath);
        if (file.exists()) {
            try {
                final ArrayList<Category> certainCategories = this.readIFCategories(filePath);
                for (final Category d : certainCategories) {
                    graph.addCategory(d);
                }
                count = certainCategories.size();
                log.info("readCertainIFCategories {} categories from {}", count, filePath);
            } catch (final Exception e) {
                log.error("Problem loading {}", file, e);
            }
        } else {
            log.warn("No {} file found", filePath);
        }
    }

    /**
     * write certain specified categories as AIMLIF files
     *
     * @param graph the Graphmaster containing the categories to write
     * @param file  the destination AIMLIF file
     */
    public boolean writeCertainIFCategories(final Graphmaster graph, final String file) {
        if (log.isTraceEnabled()) {
            log.trace("writeCertainIFCaegories {} size={}", file, graph.getCategories().size());
        }
        this.writeIFCategories(graph.getCategories(), file + this.configuration.getAimlifFileSuffix());
        final File dir = new File(this.aimlifPath);
        return dir.setLastModified(new Date().getTime());
    }

    /**
     * write learned categories to AIMLIF file
     */
    public boolean writeLearnfIFCategories() {
        return this.writeCertainIFCategories(this.learnfGraph, Constants.learnfAimlFile);
    }

    /**
     * write categories to AIMLIF file
     *
     * @param cats     array list of categories
     * @param filename AIMLIF filename
     */
    private void writeIFCategories(final ArrayList<Category> cats, final String filename) {
        final File existsPath = new File(this.aimlifPath);
        if (existsPath.exists()) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(this.aimlifPath + "/" + filename))) {
                for (final Category category : cats) {
                    writer.write(Category.categoryToIF(category));
                    writer.newLine();
                }
            } catch (final Exception e) {
                log.error("writeIFCategories problem {}", filename, e);
            }
        }
    }

    /**
     * Write all AIMLIF files from bot brain
     */
    public boolean writeAIMLIFFiles() {
        if (log.isTraceEnabled()) {
            log.trace("writeAIMLIFFiles");
        }

        final HashMap<String, BufferedWriter> fileMap = new HashMap<>();
        final Category build = new Category(this, 0, "BRAIN BUILD", "*", "*", new Date().toString(), "update.aiml");
        this.brain.addCategory(build);
        final ArrayList<Category> brainCategories = this.brain.getCategories();
        brainCategories.sort(Category.CATEGORY_NUMBER_COMPARATOR);

        final File existsPath = new File(this.aimlifPath);
        if (existsPath.exists()) {
            for (final Category c : brainCategories) {
                try {
                    BufferedWriter bw;
                    final String fileName = c.getFilename();
                    if (fileMap.containsKey(fileName)) {
                        bw = fileMap.get(fileName);
                    } else {
                        bw = new BufferedWriter(new FileWriter(this.aimlifPath + "/" + fileName + this.configuration.getAimlifFileSuffix()));
                        fileMap.put(fileName, bw);
                    }
                    bw.write(Category.categoryToIF(c));
                    bw.newLine();
                } catch (final Exception e) {
                    log.error("Error: ", e);
                }
            }
            for (final String set : fileMap.keySet()) {
                final BufferedWriter bw = fileMap.get(set);
                //Close the bw
                try {
                    if (bw != null) {
                        bw.flush();
                        bw.close();
                    }
                } catch (final IOException e) {
                    log.error("Error closing writer {}", set, e);
                }
            }
            return existsPath.setLastModified(new Date().getTime());
        }
        return false;
    }

    /**
     * Write all AIML files.  Adds categories for BUILD and DEVELOPMENT ENVIRONMENT
     */
    public boolean writeAIMLFiles() {
        if (log.isTraceEnabled()) {
            log.trace("writeAIMLFiles");
        }
        final HashMap<String, BufferedWriter> fileMap = new HashMap<>();
        final Category build = new Category(this, 0, "BRAIN BUILD", "*", "*", new Date().toString(), "update.aiml");
        this.brain.addCategory(build);

        final ArrayList<Category> brainCategories = this.brain.getCategories();
        brainCategories.sort(Category.CATEGORY_NUMBER_COMPARATOR);
        for (final Category c : brainCategories) {
            if (!c.getFilename().equals(Constants.nullAimlFile)) {
                try {
                    BufferedWriter bw;
                    final String fileName = c.getFilename();
                    if (fileMap.containsKey(fileName)) {
                        bw = fileMap.get(fileName);
                    } else {
                        final String copyright = Utilities.getCopyright(this, fileName);
                        bw = new BufferedWriter(new FileWriter(this.aimlPath + "/" + fileName));
                        fileMap.put(fileName, bw);
                        bw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
                                "<aiml>\n");
                        bw.write(copyright);
                        //bw.newLine();
                    }
                    bw.write(Category.categoryToAIML(c) + "\n");
                    //bw.newLine();
                } catch (final Exception e) {
                    log.error("Error: ", e);
                }
            }
        }
        for (final String set : fileMap.keySet()) {
            final BufferedWriter bw = fileMap.get(set);
            //Close the bw
            try {
                if (bw != null) {
                    bw.write("</aiml>\n");
                    bw.flush();
                    bw.close();
                }
            } catch (final IOException e) {
                log.error("Error closing writer {}", set, e);
            }
        }
        final File dir = new File(this.aimlPath);
        return dir.setLastModified(new Date().getTime());
    }

    /**
     * load bot properties
     */
    private void addProperties() {
        try {
            this.properties.getProperties(this.configPath + "/properties.txt");
        } catch (final Exception e) {
            log.error("Error reading properties {}", e);
        }
    }

    /**
     * read AIMLIF categories from a file into bot brain
     *
     * @param filename name of AIMLIF file
     * @return array list of categories read
     */
    private ArrayList<Category> readIFCategories(final String filename) {
        final ArrayList<Category> categories = new ArrayList<>();
        try (FileInputStream fstream = new FileInputStream(filename)) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(fstream))) {
                String strLine;
                while ((strLine = br.readLine()) != null) {
                    try {
                        final Category c = Category.IFToCategory(this, strLine);
                        categories.add(c);
                    } catch (final Exception e) {
                        log.error("Invalid AIMLIF in {} line {}", filename, strLine);
                    }
                }
            }
        } catch (final Exception e) {
            log.error("Error: ", e);
        }
        return categories;
    }

    public void deleteLearnfCategories() {
        final ArrayList<Category> learnfCategories = this.learnfGraph.getCategories();
        this.deleteLearnCategories(learnfCategories);
        this.learnfGraph = new Graphmaster(this);
    }

    public void deleteLearnCategories() {
        final ArrayList<Category> learnCategories = this.learnGraph.getCategories();
        this.deleteLearnCategories(learnCategories);
        this.learnGraph = new Graphmaster(this);
    }

    private void deleteLearnCategories(final ArrayList<Category> learnCategories) {
        for (final Category c : learnCategories) {
            final Nodemapper n = this.brain.findNode(c);
            log.info("Found node {} for {}", n, c.inputThatTopic());
            if (n != null) {
                n.setCategory(null);
            }
        }
    }

    /**
     * check Graphmaster for shadowed categories
     */
    public void shadowChecker() {
        this.shadowChecker(this.brain.getRoot());
    }

    /**
     * traverse graph and test all categories found in leaf nodes for shadows
     *
     * @param node Node mapper
     */
    private void shadowChecker(final Nodemapper node) {
        if (NodemapperOperator.isLeaf(node)) {
            String input = node.getCategory().getPattern();
            input = this.brain.replaceBotProperties(input);
            input = input
                    .replace("*", "XXX")
                    .replace("_", "XXX")
                    .replace("^", "")
                    .replace("#", "");
            final String that = node.getCategory().getThat()
                    .replace("*", "XXX")
                    .replace("_", "XXX")
                    .replace("^", "")
                    .replace("#", "");
            final String topic = node.getCategory().getTopic()
                    .replace("*", "XXX")
                    .replace("_", "XXX")
                    .replace("^", "")
                    .replace("#", "");
            input = this.instantiateSets(input);
            log.debug("shadowChecker: input={}", input);
            final Nodemapper match = this.brain.match(input, that, topic);
            if (match != node) {
                log.debug("             {}", Graphmaster.inputThatTopic(input, that, topic));
                log.debug("MATCHED:     {}", match.getCategory().inputThatTopic());
                log.debug("SHOULD MATCH:{}", node.getCategory().inputThatTopic());
            }
        } else {
            for (final String key : NodemapperOperator.keySet(node)) {
                this.shadowChecker(NodemapperOperator.get(node, key));
            }
        }
    }

    private String instantiateSets(final String pattern) {
        final String[] splitPattern = pattern.split(" ");
        final StringBuilder builder = new StringBuilder();
        for (String x : splitPattern) {
            if (x.startsWith("<SET>")) {
                final String setName = AIMLProcessor.trimTag(x, "SET");
                final AIMLSet set = this.sets.get(setName);
                x = set != null ? "FOUNDITEM" : "NOTFOUND";
            }
            builder.append(" ").append(x);
        }
        return builder.toString().trim();
    }
}
