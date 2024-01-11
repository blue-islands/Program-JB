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

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Clause {
    private String subj;
    private String pred;
    private String obj;
    private Boolean affirm;

    public Clause(final String s, final String p, final String o) {
        this(s, p, o, true);
    }

    public Clause(final String s, final String p, final String o, final Boolean affirm) {
        this.subj = s;
        this.pred = p;
        this.obj = o;
        this.affirm = affirm;
    }

    public Clause(final Clause clause) {
        this(clause.subj, clause.pred, clause.obj, clause.affirm);
    }
}
