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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bot Properties
 */
public class Properties extends HashMap<String, String> {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(Properties.class);

    /**
     * get the value of a bot property.
     *
     * @param key property name
     * @return property value or a string indicating the property is undefined
     */
    public String get(final String key) {
        final String result = super.get(key);
        return result != null ? result : Constants.default_property;
    }

    /**
     * Read bot properties from an input stream.
     *
     * @param in Input stream
     */
    public int getPropertiesFromInputStream(final InputStream in) {
        int cnt = 0;
        String strLine;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF8"))) {
            while ((strLine = br.readLine()) != null) {
                if (strLine.contains(":")) {
                    final String property = strLine.substring(0, strLine.indexOf(":"));
                    final String value = strLine.substring(strLine.indexOf(":") + 1);
                    this.put(property, value);
                    cnt++;
                }
            }
        } catch (final Exception e) {
            log.error("Error:", e);
        }
        return cnt;
    }

    /**
     * Read bot properties from a file.
     *
     * @param filename file containing bot properties
     */
    public int getProperties(final String filename) {
        int cnt = 0;
        if (log.isTraceEnabled()) {
            log.trace("Get Properties: {}", filename);
        }
        try {
            // Open the file that is the first
            // command line parameter
            final File file = new File(filename);
            if (file.exists()) {
                try (FileInputStream fstream = new FileInputStream(filename)) {
                    cnt = this.getPropertiesFromInputStream(fstream);
                }
            }
        } catch (final Exception e) {
            log.error("Error:", e);
        }
        return cnt;
    }
}
