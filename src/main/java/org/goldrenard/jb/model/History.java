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

import org.goldrenard.jb.configuration.Constants;

import lombok.Getter;

/**
 * History object to maintain history of input, that request and response
 *
 * @param <T> type of history object
 */
@Getter
public class History<T> {
    private final Object[] history;
    private final String name;

    /**
     * Constructor with default history name
     */
    public History(final int maxHistory) {
        this(maxHistory, "unknown");
    }

    /**
     * Constructor with history name
     *
     * @param name name of history
     */
    public History(final int maxHistory, final String name) {
        this.name = name;
        this.history = new Object[maxHistory];
    }

    /**
     * add an item to history
     *
     * @param item history item to add
     */
    public void add(final T item) {
        for (int i = this.history.length - 1; i > 0; i--) {
            this.history[i] = this.history[i - 1];
        }
        this.history[0] = item;
    }

    /**
     * get an item from history
     *
     * @param index history index
     * @return history item
     */
    @SuppressWarnings("unchecked")
    public T get(final int index) {
        if (index < this.history.length) {
            return (T) this.history[index];
        }
        return null;
    }

    /**
     * get a String history item
     *
     * @param index history index
     * @return history item
     */
    public String getString(final int index) {
        if (index < this.history.length) {
            if (this.history[index] == null) {
                return Constants.unknown_history_item;
            }
            return (String) this.history[index];
        }
        return null;
    }
}
