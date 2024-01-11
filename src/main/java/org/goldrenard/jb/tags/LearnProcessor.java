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

import org.goldrenard.jb.configuration.Constants;
import org.goldrenard.jb.core.AIMLProcessor;
import org.goldrenard.jb.core.Bot;
import org.goldrenard.jb.model.Category;
import org.goldrenard.jb.model.ParseState;
import org.goldrenard.jb.tags.base.BaseTagProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class LearnProcessor extends BaseTagProcessor {

    private static final Logger log = LoggerFactory.getLogger(LearnProcessor.class);

    public LearnProcessor() {
        super("learn", "learnf");
    }

    @Override
    public String eval(final Node node, final ParseState ps) {
        final NodeList childList = node.getChildNodes();
        String pattern = "";
        String that = "*";
        String template = "";
        for (int i = 0; i < childList.getLength(); i++) {
            if (childList.item(i).getNodeName().equals("category")) {
                final NodeList grandChildList = childList.item(i).getChildNodes();
                for (int j = 0; j < grandChildList.getLength(); j++) {
                    if (grandChildList.item(j).getNodeName().equals("pattern")) {
                        pattern = this.recursLearn(grandChildList.item(j), ps);
                    } else if (grandChildList.item(j).getNodeName().equals("that")) {
                        that = this.recursLearn(grandChildList.item(j), ps);
                    } else if (grandChildList.item(j).getNodeName().equals("template")) {
                        template = this.recursLearn(grandChildList.item(j), ps);
                    }
                }
                pattern = pattern.substring("<pattern>".length(), pattern.length() - "</pattern>".length());
                if (log.isTraceEnabled()) {
                    log.trace("Learn Pattern = {}", pattern);
                }
                if (template.length() >= "<template></template>".length()) {
                    template = template.substring("<template>".length(), template.length() - "</template>".length());
                }
                if (that.length() >= "<that></that>".length()) {
                    that = that.substring("<that>".length(), that.length() - "</that>".length());
                }
                pattern = pattern.toUpperCase();
                pattern = pattern.replaceAll("\n", " ");
                pattern = pattern.replaceAll("[ ]+", " ");
                that = that.toUpperCase();
                that = that.replaceAll("\n", " ");
                that = that.replaceAll("[ ]+", " ");
                if (log.isTraceEnabled()) {
                    log.trace("Learn Pattern = {}", pattern);
                    log.trace("Learn That = {}", that);
                    log.trace("Learn Template = {}", template);
                }
                final Bot bot = ps.getChatSession().getBot();
                Category c;
                if (node.getNodeName().equals("learn")) {
                    c = new Category(bot, 0, pattern, that, "*", template, Constants.nullAimlFile);
                    bot.getLearnGraph().addCategory(c);
                } else {// learnf
                    c = new Category(bot, 0, pattern, that, "*", template, Constants.learnfAimlFile);
                    bot.getLearnfGraph().addCategory(c);
                }
                ps.getChatSession().getBot().getBrain().addCategory(c);
            }
        }
        return "";
    }

    private String unevaluatedAIML(final Node node, final ParseState ps) {
        final String result = this.learnEvalTagContent(node, ps);
        return AIMLProcessor.unevaluatedXML(result, node, ps);
    }

    private String recursLearn(final Node node, final ParseState ps) {
        final String nodeName = node.getNodeName();
        if ("#text".equals(nodeName)) {
            return node.getNodeValue();
        } else if ("eval".equals(nodeName)) {
            return this.evalTagContent(node, ps, null);                // AIML 2.0
        }
        return this.unevaluatedAIML(node, ps);
    }

    private String learnEvalTagContent(final Node node, final ParseState ps) {
        final NodeList childList = node.getChildNodes();
        final StringBuilder result = new StringBuilder();
        for (int i = 0; i < childList.getLength(); i++) {
            final Node child = childList.item(i);
            result.append(this.recursLearn(child, ps));
        }
        return result.toString();
    }
}
