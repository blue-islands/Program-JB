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
package org.goldrenard.jb.parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.goldrenard.jb.core.Bot;
import org.goldrenard.jb.model.Substitution;
import org.goldrenard.jb.parser.base.CollectionResource;
import org.goldrenard.jb.utils.Utilities;

public class SubstitutionResource extends CollectionResource<Substitution> {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private final static Pattern ENTRY_PATTERN = Pattern.compile("\"(.*?)\",\"(.*?)\"", Pattern.DOTALL);

    private final Bot bot;

    public SubstitutionResource(final Bot bot) {
        this.bot = bot;
    }

    @Override
    public int read(final String filePath) {
        Utilities.readFileLines(filePath)
                .stream()
                .filter(StringUtils::isNotEmpty)
                .forEach(e -> {
                    if (this.size() < this.bot.getConfiguration().getMaxSubstitutions()) {
                        final Substitution substitution = this.parse(e);
                        if (substitution != null) {
                            this.add(substitution);
                        }
                    }
                });
        return this.size();
    }

    private Substitution parse(final String input) {
        Substitution substitution = null;
        final Matcher matcher = ENTRY_PATTERN.matcher(input);
        if (matcher.find()) {
            substitution = new Substitution();
            final String quotedPattern = Pattern.quote(matcher.group(1));
            substitution.setSubstitution(matcher.group(2));
            substitution.setPattern(Pattern.compile(quotedPattern, Pattern.CASE_INSENSITIVE));
        }
        return substitution;
    }
}
