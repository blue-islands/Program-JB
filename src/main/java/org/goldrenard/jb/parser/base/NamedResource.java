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
package org.goldrenard.jb.parser.base;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.goldrenard.jb.model.NamedEntity;
import org.goldrenard.jb.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class NamedResource<T extends NamedEntity> extends HashMap<String, T> implements ParsedResource<T>, Map<String, T> {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(NamedResource.class);

    private final String resourceExtension;

    protected NamedResource(final String resourceExtension) {
        Objects.requireNonNull(resourceExtension, "Resource extension is required");
        this.resourceExtension = resourceExtension;
    }

    @Override
    public int read(final String path) {
        int count = 0;
        try {
            final File folder = new File(path);
            if (folder.exists()) {
                if (log.isTraceEnabled()) {
                    log.trace("Loading resources files from {}", path);
                }
                for (final File file : IOUtils.listFiles(folder)) {
                    if (file.isFile() && file.exists()) {
                        final String fileName = file.getName();
                        final String extension = FilenameUtils.getExtension(fileName);
                        if (this.resourceExtension.equalsIgnoreCase(extension)) {
                            final String resourceName = FilenameUtils.getBaseName(fileName);
                            if (log.isTraceEnabled()) {
                                log.trace("Read AIML resource {} from {}", resourceName, fileName);
                            }
                            final T entry = this.load(resourceName, file);
                            if (entry instanceof Set) {
                                count += ((Set) entry).size();
                            }
                            if (entry instanceof Map) {
                                count += ((Map) entry).size();
                            }
                            this.put(entry.getName(), entry);
                        }
                    }
                }
            } else {
                log.warn("{} does not exist.", path);
            }
        } catch (final Exception e) {
            log.error("Error: ", e);
        }
        return count;
    }

    protected abstract T load(String resourceName, File file);

    @Override
    public void write(final Collection<T> resources) {
        for (final T resource : resources) {
            try {
                this.write(resource);
            } catch (final Exception e) {
                log.error("Could not write resource {}", resource);
            }
        }
    }
}
