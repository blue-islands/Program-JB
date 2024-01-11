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
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.goldrenard.jb.configuration.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Tuple extends HashMap<String, String> {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(Tuple.class);

    private static AtomicLong index = new AtomicLong();
    private Set<String> visibleVars = new HashSet<>();
    private String name;

    protected Tuple(final Set<String> varSet, final Set<String> visibleVars, final Tuple tuple) {
        if (visibleVars != null) {
            this.visibleVars.addAll(visibleVars);
        }
        if (varSet == null && tuple != null) {
            for (final String key : tuple.keySet()) {
                this.put(key, tuple.get(key));
            }
            this.visibleVars.addAll(tuple.visibleVars);
        }
        if (varSet != null) {
            for (final String key : varSet) {
                this.put(key, Constants.unbound_variable);
            }
        }
        this.name = "tuple" + index.incrementAndGet();
    }

    public Tuple(final Tuple tuple) {
        this(null, null, tuple);
    }

    public Tuple(final Set<String> varSet, final Set<String> visibleVars) {
        this(varSet, visibleVars, null);
    }

    public Set<String> getVars() {
        return this.keySet();
    }

    public String getValue(final String var) {
        final String result = this.get(var);
        if (result == null) {
            return Constants.default_get;
        }
        return result;
    }

    public void bind(final String var, final String value) {
        if (this.get(var) != null && !this.get(var).equals(Constants.unbound_variable)) {
            log.warn("{} already bound to {}", var, this.get(var));
        } else {
            this.put(var, value);
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (o == null || !Tuple.class.isAssignableFrom(o.getClass())) {
            return false;
        }
        final Tuple tuple = (Tuple) o;
        if (this.visibleVars.size() != tuple.visibleVars.size()) {
            return false;
        }
        for (final String x : this.visibleVars) {
            if (!tuple.visibleVars.contains(x)) {
                return false;
            } else if (this.get(x) != null && !this.get(x).equals(tuple.get(x))) {
                return false;
            }
        }
        return !this.values().contains(Constants.unbound_variable)
                && !tuple.values().contains(Constants.unbound_variable);
    }

    @Override
    public int hashCode() {
        int result = 1;
        for (final String x : this.visibleVars) {
            result = 31 * result + x.hashCode();
            if (this.get(x) != null) {
                result = 31 * result + this.get(x).hashCode();
            }
        }
        return result;
    }
}
