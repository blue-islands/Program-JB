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

import java.util.HashMap;

import org.goldrenard.jb.configuration.Constants;
import org.goldrenard.jb.core.Bot;
import org.goldrenard.jb.core.Sraix;
import org.goldrenard.jb.i18n.Inflector;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * implements AIML Map
 * <p>
 * A map is a function from one string set to another.
 * Elements of the domain are called keys and elements of the range are called values.
 */
@Getter
@Setter
@ToString
public class AIMLMap extends HashMap<String, String> implements NamedEntity {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private final Bot bot;

    private final String name;

    private String host;    // for external maps
    private String botId;   // for external maps
    private boolean external = false;

    /**
     * constructor to create a new AIML Map
     *
     * @param name the name of the map
     */
    public AIMLMap(final String name, final Bot bot) {
        this.bot = bot;
        this.name = name;
    }

    /**
     * return a map value given a key
     *
     * @param key the domain element
     * @return the range element or a string indicating the key was not found
     */
    public String get(final String key) {
        String value;
        if (this.name.equals(Constants.map_successor)) {
            try {
                final int number = Integer.parseInt(key);
                return String.valueOf(number + 1);
            } catch (final Exception e) {
                return Constants.default_map;
            }
        } else if (this.name.equals(Constants.map_predecessor)) {
            try {
                final int number = Integer.parseInt(key);
                return String.valueOf(number - 1);
            } catch (final Exception e) {
                return Constants.default_map;
            }
        } else if (this.name.equals("singular")) {
            return Inflector.getInstance().singularize(key).toLowerCase();
        } else if (this.name.equals("plural")) {
            return Inflector.getInstance().pluralize(key).toLowerCase();
        } else if (this.external && this.bot.getConfiguration().isEnableExternalMaps()) {
            //String[] split = key.split(" ");
            final String query = this.name.toUpperCase() + " " + key;
            final String response = Sraix.sraix(null, null, this.bot, query, Constants.default_map, null, this.host, this.botId,
                    null, "0");
            value = response;
        } else {
            value = super.get(key);
        }
        if (value == null) {
            value = Constants.default_map;
        }

        return value;
    }
}
