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
package org.goldrenard.jb.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

import org.goldrenard.jb.configuration.Constants;
import org.goldrenard.jb.core.Bot;
import org.goldrenard.jb.utils.JapaneseUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manage client predicates
 */
public class Predicates extends HashMap<String, String> {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(Predicates.class);

    private final Bot bot;

    public Predicates(final Bot bot) {
        this.bot = bot;
    }

    /**
     * save a predicate value
     *
     * @param key   predicate name
     * @param value predicate value
     * @return predicate value
     */
    @Override
    public String put(final String key, String value) {
        if (this.bot.getConfiguration().isJpTokenize()) {
            if (key.equals("topic")) {
                value = JapaneseUtils.tokenizeSentence(value);
            }
        }
        if (key.equals("topic") && value.length() == 0) {
            value = Constants.default_get;
        }
        if (value.equals(this.bot.getConfiguration().getLanguage().getTooMuchRecursion())) {
            value = Constants.default_list_item;
        }
        return super.put(key, value);
    }

    /**
     * get a predicate value
     *
     * @param key predicate name
     * @return predicate value
     */
    public String get(final String key) {
        final String result = super.get(key);
        return result != null ? result : Constants.default_get;
    }

    /**
     * Read predicate default values from an input stream
     *
     * @param in input stream
     */
    private void getPredicateDefaultsFromInputStream(final InputStream in) {
        String strLine;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
            //Read File Line By Line
            while ((strLine = br.readLine()) != null) {
                if (strLine.contains(":")) {
                    final String property = strLine.substring(0, strLine.indexOf(":"));
                    final String value = strLine.substring(strLine.indexOf(":") + 1);
                    this.put(property, value);
                }
            }
        } catch (final Exception e) {
            log.error("Error: ", e);
        }
    }

    /**
     * read predicate defaults from a file
     *
     * @param filename name of file
     */
    public void getPredicateDefaults(final String filename) {
        try {
            // Open the file that is the first
            // command line parameter
            final File file = new File(filename);
            if (file.exists()) {
                try (FileInputStream stream = new FileInputStream(filename)) {
                    this.getPredicateDefaultsFromInputStream(stream);
                }
            }
        } catch (final Exception e) {
            log.error("Error: ", e);
        }
    }
}


