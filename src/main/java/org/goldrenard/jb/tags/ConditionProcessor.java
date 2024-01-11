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

import java.util.ArrayList;
import java.util.Set;

import org.goldrenard.jb.core.Bot;
import org.goldrenard.jb.model.ParseState;
import org.goldrenard.jb.tags.base.BaseTagProcessor;
import org.goldrenard.jb.utils.Utilities;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * implements all 3 forms of the {@code <condition> tag}
 * In AIML 2.0 the conditional may return a {@code <loop/>}
 */
public class ConditionProcessor extends BaseTagProcessor {

    public ConditionProcessor() {
        super("condition");
    }

    private String condition(final Node node, final ParseState ps) {
        final NodeList childList = node.getChildNodes();
        final ArrayList<Node> liList = new ArrayList<>();
        String predicate, varName, value; //Node p=null, v=null;
        final Set<String> attributeNames = Utilities.stringSet("name", "var", "value");
        // First check if the <condition> has an attribute "name".  If so, get the predicate name.
        predicate = this.getAttributeOrTagValue(node, ps, "name");
        varName = this.getAttributeOrTagValue(node, ps, "var");
        // Make a list of all the <li> child nodes:
        for (int i = 0; i < childList.getLength(); i++) {
            if (childList.item(i).getNodeName().equals("li")) {
                liList.add(childList.item(i));
            }
        }
        if (liList.size() == 0 && (value = this.getAttributeOrTagValue(node, ps, "value")) != null &&
                predicate != null &&
                ps.getChatSession().getPredicates().get(predicate).equalsIgnoreCase(value)) {
            return this.evalTagContent(node, ps, attributeNames);
        } else if (liList.size() == 0 && (value = this.getAttributeOrTagValue(node, ps, "value")) != null &&
                varName != null &&
                ps.getVars().get(varName).equalsIgnoreCase(value)) {
            return this.evalTagContent(node, ps, attributeNames);
        } else {
            for (final Node n : liList) {
                String liPredicate = predicate;
                String liVarName = varName;
                if (liPredicate == null) {
                    liPredicate = this.getAttributeOrTagValue(n, ps, "name");
                }
                if (liVarName == null) {
                    liVarName = this.getAttributeOrTagValue(n, ps, "var");
                }
                value = this.getAttributeOrTagValue(n, ps, "value");
                if (value != null) {
                    // if the predicate equals the value, return the <li> item.
                    if (liPredicate != null && (ps.getChatSession().getPredicates().get(liPredicate).equalsIgnoreCase(value) ||
                            (ps.getChatSession().getPredicates().containsKey(liPredicate) && value.equals("*")))) {
                        return this.evalTagContent(n, ps, attributeNames);
                    } else if (liVarName != null && (ps.getVars().get(liVarName).equalsIgnoreCase(value) ||
                            (ps.getVars().containsKey(liPredicate) && value.equals("*")))) {
                        return this.evalTagContent(n, ps, attributeNames);
                    }
                } else {
                    return this.evalTagContent(n, ps, attributeNames);
                }
            }
        }
        return "";
    }

    @Override
    public String eval(final Node node, final ParseState ps) {
        final Bot bot = ps.getChatSession().getBot();
        boolean loop = true;
        final StringBuilder result = new StringBuilder();
        final int loopCnt = 0;
        while (loop && loopCnt < bot.getConfiguration().getMaxLoops()) {
            String loopResult = this.condition(node, ps);
            final String tooMuch = bot.getConfiguration().getLanguage().getTooMuchRecursion();
            if (loopResult.trim().equals(tooMuch)) {
                return tooMuch;
            }
            if (loopResult.contains("<loop/>")) {
                loopResult = loopResult.replace("<loop/>", "");
                loop = true;
            } else {
                loop = false;
            }
            result.append(loopResult);
        }
        return loopCnt >= bot.getConfiguration().getMaxLoops()
                ? bot.getConfiguration().getLanguage().getTooMuchLooping()
                : result.toString();
    }
}
