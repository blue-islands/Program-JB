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
package org.goldrenard.jb.tags;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.goldrenard.jb.model.Clause;
import org.goldrenard.jb.model.ParseState;
import org.goldrenard.jb.model.Tuple;
import org.goldrenard.jb.tags.base.BaseTagProcessor;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class UniqProcessor extends BaseTagProcessor {

    public UniqProcessor() {
        super("uniq");
    }

    @Override
    public String eval(final Node node, final ParseState ps) {
        final HashSet<String> vars = new HashSet<>();
        final HashSet<String> visibleVars = new HashSet<>();
        String subj = "?subject";
        String pred = "?predicate";
        String obj = "?object";
        final NodeList childList = node.getChildNodes();
        for (int j = 0; j < childList.getLength(); j++) {
            final Node childNode = childList.item(j);
            final String contents = this.evalTagContent(childNode, ps, null);
            if (childNode.getNodeName().equals("subj")) {
                subj = contents;
            } else if (childNode.getNodeName().equals("pred")) {
                pred = contents;
            } else if (childNode.getNodeName().equals("obj")) {
                obj = contents;
            }
            if (contents.startsWith("?")) {
                visibleVars.add(contents);
                vars.add(contents);
            }
        }
        final Tuple partial = ps.getChatSession().getTripleStore().storeTuple(new Tuple(vars, visibleVars));
        final Clause clause = new Clause(subj, pred, obj);
        final Set<Tuple> tuples = ps.getChatSession().getTripleStore().selectFromSingleClause(partial, clause, true);
        String tupleList = tuples.stream().map(Tuple::getName).collect(Collectors.joining(" "));
        if (tupleList.length() == 0) {
            tupleList = "NIL";
        }
        String var = "";
        for (final String x : visibleVars) {
            var = x;
        }
        final String firstTuple = firstWord(tupleList);
        return ps.getChatSession().getTripleStore().tupleGet(firstTuple, var);
    }
}
