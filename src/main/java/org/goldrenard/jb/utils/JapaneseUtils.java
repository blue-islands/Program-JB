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
package org.goldrenard.jb.utils;

import java.util.List;
import java.util.stream.Collectors;

import org.goldrenard.jb.core.AIMLProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.atilika.kuromoji.ipadic.Token;
import com.atilika.kuromoji.ipadic.Tokenizer;

public class JapaneseUtils {

    private static final Logger    log       = LoggerFactory.getLogger(JapaneseUtils.class);

    private static final Tokenizer tokenizer = new Tokenizer();

    /**
     * Tokenize a fragment of the input that contains only text
     *
     * @param fragment fragment of input containing only text and no XML tags
     * @return tokenized fragment
     */
    private static String tokenizeFragment(final String fragment) {

        if (JapaneseUtils.log.isDebugEnabled()) {
            JapaneseUtils.log.debug("buildFragment: {}", fragment);
        }
        final List <Token> tokens = JapaneseUtils.tokenizer.tokenize(fragment);
        return tokens.stream().map(Token::getSurface).collect(Collectors.joining(" "));
    }


    /**
     * Morphological analysis of an input sentence that contains an AIML pattern.
     *
     * @param sentence
     * @return morphed sentence with one space between words, preserving XML markup and AIML $ operation
     */
    public static String tokenizeSentence(final String sentence) {

        if (JapaneseUtils.log.isDebugEnabled()) {
            JapaneseUtils.log.debug("tokenizeSentence: {}", sentence);
        }
        String result = "";
        result = JapaneseUtils.tokenizeXML(sentence);
        if (result != null) {
            while (result.contains("$ ")) {
                result = result.replace("$ ", "$");
            }
            while (result.contains("  ")) {
                result = result.replace("  ", " ");
            }
            while (result.contains("anon ")) {
                result = result.replace("anon ", "anon"); // for Triple Store
            }
            result = result.trim();
            if (JapaneseUtils.log.isTraceEnabled()) {
                JapaneseUtils.log.trace("tokenizeSentence: {} --> result: {}", sentence, result);
            }
        }
        return result;
    }


    private static String tokenizeXML(String xmlExpression) {

        if (JapaneseUtils.log.isDebugEnabled()) {
            JapaneseUtils.log.debug("tokenizeXML: {}", xmlExpression);
        }
        String response = "";
        try {
            xmlExpression = "<sentence>" + xmlExpression + "</sentence>";
            final Node root = DomUtils.parseString(xmlExpression);
            response = JapaneseUtils.recursEval(root);
        } catch (final Exception e) {
            JapaneseUtils.log.error("Error:", e);
        }
        return AIMLProcessor.trimTag(response, "sentence");
    }


    private static String recursEval(final Node node) {

        try {
            final String nodeName = node.getNodeName();
            if (JapaneseUtils.log.isDebugEnabled()) {
                JapaneseUtils.log.debug("recursEval: {}", nodeName);
            }
            switch (nodeName) {
                case "#text":
                    return JapaneseUtils.tokenizeFragment(node.getNodeValue());
                case "sentence":
                    return JapaneseUtils.evalTagContent(node);
                default:
                    return JapaneseUtils.genericXML(node);
            }
        } catch (final Exception e) {
            JapaneseUtils.log.debug("recursEval failed", e);
        }
        return "JP Morph Error";
    }


    private static String genericXML(final Node node) {

        if (JapaneseUtils.log.isDebugEnabled()) {
            JapaneseUtils.log.debug("genericXML: {}", node.getNodeName());
        }
        final String result = JapaneseUtils.evalTagContent(node);
        return JapaneseUtils.unevaluatedXML(result, node);
    }


    private static String evalTagContent(final Node node) {

        if (JapaneseUtils.log.isDebugEnabled()) {
            JapaneseUtils.log.debug("evalTagContent: {}", node.getNodeName());
        }
        final StringBuilder result = new StringBuilder();
        try {
            final NodeList childList = node.getChildNodes();
            for (int i = 0; i < childList.getLength(); i++) {
                final Node child = childList.item(i);
                result.append(JapaneseUtils.recursEval(child));
            }
        } catch (final Exception e) {
            JapaneseUtils.log.warn("Something went wrong with evalTagContent", e);
        }
        return result.toString();
    }


    private static String unevaluatedXML(final String result, final Node node) {

        final String nodeName = node.getNodeName();
        final StringBuilder attributes = new StringBuilder();
        if (node.hasAttributes()) {
            final NamedNodeMap XMLAttributes = node.getAttributes();
            for (int i = 0; i < XMLAttributes.getLength(); i++) {
                attributes
                        .append(" ")
                        .append(XMLAttributes.item(i).getNodeName())
                        .append("=\"")
                        .append(XMLAttributes.item(i).getNodeValue())
                        .append("\"");
            }
        }
        if ("".equals(result)) {
            return " <" + nodeName + attributes + "/> ";
        }
        return " <" + nodeName + attributes + ">" + result + "</" + nodeName + "> "; // add spaces
    }
}
