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
package org.goldrenard.jb.tags.base;

import java.util.Set;

import org.goldrenard.jb.configuration.Constants;
import org.goldrenard.jb.core.AIMLProcessor;
import org.goldrenard.jb.model.ParseState;
import org.goldrenard.jb.utils.DomUtils;
import org.goldrenard.jb.utils.Utilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class BaseTagProcessor implements AIMLTagProcessor {

    private static final Logger log = LoggerFactory.getLogger(BaseTagProcessor.class);

    protected AIMLProcessor processor;

    @Getter
    protected final Set<String> tags;

    protected BaseTagProcessor(final String... tags) {
        this.tags = Utilities.stringSet(tags);
    }

    /**
     * in AIML 2.0, an attribute value can be specified by either an XML attribute value
     * or a subtag of the same name.  This function tries to read the value from the XML attribute first,
     * then tries to look for the subtag.
     *
     * @param node          current parse node.
     * @param ps            current parse state.
     * @param attributeName the name of the attribute.
     * @return the attribute value.
     */
    // value can be specified by either attribute or tag
    protected String getAttributeOrTagValue(final Node node, final ParseState ps, final String attributeName) {
        if (log.isTraceEnabled()) {
            log.trace("BaseTagProcessor.getAttributeOrTagValue (node: {}, attributeName: {})", node, attributeName);
        }
        String result;
        final Node m = node.getAttributes().getNamedItem(attributeName);
        if (m == null) {
            final NodeList childList = node.getChildNodes();
            result = null;         // no attribute or tag named attributeName
            for (int i = 0; i < childList.getLength(); i++) {
                final Node child = childList.item(i);
                if (log.isTraceEnabled()) {
                    log.trace("getAttributeOrTagValue child = {}", child.getNodeName());
                }
                if (child.getNodeName().equals(attributeName)) {
                    result = this.evalTagContent(child, ps, null);
                    if (log.isTraceEnabled()) {
                        log.trace("getAttributeOrTagValue result from child = {}", result);
                    }
                }
            }
        } else {
            result = m.getNodeValue();
        }
        if (log.isTraceEnabled()) {
            log.trace("in BaseTagProcessor.getAttributeOrTagValue (), returning: {}", result);
        }
        return result;
    }

    /**
     * evaluate the contents of an AIML tag.
     * calls recursEval on child tags.
     *
     * @param node             the current parse node.
     * @param ps               the current parse state.
     * @param ignoreAttributes tag names to ignore when evaluating the tag.
     * @return the result of evaluating the tag contents.
     */
    protected String evalTagContent(final Node node, final ParseState ps, final Set<String> ignoreAttributes) {
        if (log.isTraceEnabled()) {
            log.trace("BaseTagProcessor.evalTagContent(node: {}, ps: {}, ignoreAttributes: {}", node, ps, ignoreAttributes);
            log.trace("in BaseTagProcessor.evalTagContent, node string: {}", DomUtils.nodeToString(node));
        }
        final StringBuilder result = new StringBuilder();
        try {
            final NodeList childList = node.getChildNodes();
            for (int i = 0; i < childList.getLength(); i++) {
                final Node child = childList.item(i);
                if (log.isTraceEnabled()) {
                    log.trace("in BaseTagProcessor.evalTagContent(), child: {}", child);
                }
                if (ignoreAttributes == null || !ignoreAttributes.contains(child.getNodeName())) {
                    result.append(ps.getProcessor().recursEval(child, ps));
                }
                if (log.isTraceEnabled()) {
                    log.trace("in BaseTagProcessor.evalTagContent(), result: ", result);
                }
            }
        } catch (final Exception e) {
            log.error("Something went wrong with evalTagContent", e);
        }

        if (log.isTraceEnabled()) {
            log.trace("BaseTagProcessor.evalTagContent() returning: {}", result);
        }
        return result.toString();
    }

    /**
     * get the value of an index attribute and return it as an integer.
     * if it is not recognized as an integer, return 0
     *
     * @param node current XML parse node
     * @param ps   AIML parse state
     * @return the the integer intex value
     */
    protected int getIndexValue(final Node node, final ParseState ps) {
        final String value = this.getAttributeOrTagValue(node, ps, "index");
        if (value != null) {
            try {
                return Integer.parseInt(value) - 1;
            } catch (final Exception e) {
                log.error("Error: ", e);
            }
        }
        return 0;
    }

    protected static String firstWord(final String sentence) {
        String content = (sentence == null ? "" : sentence);
        content = content.trim();
        if (content.contains(" ")) {
            return content.substring(0, content.indexOf(" "));
        } else if (content.length() > 0) {
            return content;
        }
        return Constants.default_list_item;
    }

}
