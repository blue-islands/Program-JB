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

import org.goldrenard.jb.model.ParseState;
import org.goldrenard.jb.tags.base.BaseTagProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

public class TripleProcessor extends BaseTagProcessor {

    private static final Logger log = LoggerFactory.getLogger(TripleProcessor.class);

    public TripleProcessor() {
        super("addtriple", "deletetriple");
    }

    @Override
    public String eval(final Node node, final ParseState ps) {
        try {
            switch (node.getNodeName()) {
                case "addtriple":
                    return this.addTriple(node, ps);
                case "deletetriple":
                    return this.deleteTriple(node, ps);
                default:
                    throw new IllegalStateException("Unsupported tag");
            }
        } catch (final Exception e) {
            log.error("Error: ", e);
            return "";
        }
    }

    private String deleteTriple(final Node node, final ParseState ps) {
        final String subject = this.getAttributeOrTagValue(node, ps, "subj");
        final String predicate = this.getAttributeOrTagValue(node, ps, "pred");
        final String object = this.getAttributeOrTagValue(node, ps, "obj");
        return ps.getChatSession().getTripleStore().deleteTriple(subject, predicate, object);
    }

    private String addTriple(final Node node, final ParseState ps) {
        final String subject = this.getAttributeOrTagValue(node, ps, "subj");
        final String predicate = this.getAttributeOrTagValue(node, ps, "pred");
        final String object = this.getAttributeOrTagValue(node, ps, "obj");
        return ps.getChatSession().getTripleStore().addTriple(subject, predicate, object);
    }
}
