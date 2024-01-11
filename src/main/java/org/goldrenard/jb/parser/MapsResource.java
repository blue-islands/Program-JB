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

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.goldrenard.jb.configuration.Constants;
import org.goldrenard.jb.core.Bot;
import org.goldrenard.jb.model.AIMLMap;
import org.goldrenard.jb.parser.base.NamedResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MapsResource extends NamedResource<AIMLMap> {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private static final String MAPS_EXTENSION = "txt";

    private static final Logger log = LoggerFactory.getLogger(MapsResource.class);

    private final Bot bot;

    public MapsResource(final Bot bot) {
        super(MAPS_EXTENSION);
        this.bot = bot;
        this.put(Constants.map_successor, new AIMLMap(Constants.map_successor, bot));
        this.put(Constants.map_predecessor, new AIMLMap(Constants.map_predecessor, bot));
        this.put(Constants.map_singular, new AIMLMap(Constants.map_singular, bot));
        this.put(Constants.map_plural, new AIMLMap(Constants.map_plural, bot));
    }

    @Override
    protected AIMLMap load(final String resourceName, final File file) {
        final AIMLMap aimlMap = new AIMLMap(resourceName, this.bot);
        try {
            for (final String line : FileUtils.readLines(file, "UTF-8")) {
                final String[] splitLine = line.split(":");
                if (log.isDebugEnabled()) {
                    log.debug("AIMLMap line={}", line);
                }
                if (splitLine.length >= 2) {
                    if (line.startsWith(Constants.remote_map_key)) {
                        if (splitLine.length >= 3) {
                            aimlMap.setHost(splitLine[1]);
                            aimlMap.setBotId(splitLine[2]);
                            aimlMap.setExternal(true);
                            log.info("Created external map at [host={}, botId={}]", aimlMap.getHost(), aimlMap.getBotId());
                        }
                    } else {
                        final String key = splitLine[0].toUpperCase();
                        final String value = splitLine[1];
                        // assume domain element is already normalized for speedier load
                        //key = bot.preProcessor.normalize(key).trim();
                        aimlMap.put(key, value);
                    }
                }
            }
        } catch (final Exception e) {
            log.error("Read AIML Set error", e);
        }
        return aimlMap;
    }

    @Override
    public void write(final AIMLMap resource) {
        log.info("Writing AIML Map {}", resource.getName());

        List<String> lines;
        if (resource.isExternal()) {
            lines = Collections.singletonList(String.format("external:%s:%s", resource.getHost(), resource.getBotId()));
        } else {
            lines = resource.keySet().stream().map(e -> {
                e = e.trim();
                return e + ":" + resource.get(e).trim();
            }).collect(Collectors.toList());
        }

        final String fileName = this.bot.getMapsPath() + "/" + resource.getName() + "." + MAPS_EXTENSION;

        try {
            FileUtils.writeLines(new File(fileName), "UTF-8", lines);
        } catch (final Exception e) {
            log.error("Error: ", e);
        }
    }
}
