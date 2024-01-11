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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IOUtils {

    private static final Logger log = LoggerFactory.getLogger(IOUtils.class);

    private BufferedReader reader;
    private BufferedWriter writer;

    public IOUtils(final String filePath, final String mode) {
        try {
            if (mode.equals("read")) {
                this.reader = new BufferedReader(new FileReader(filePath));
            } else if (mode.equals("write")) {
                if (!new File(filePath).delete()) {
                    log.warn("Could not delete {}", filePath);
                }
                this.writer = new BufferedWriter(new FileWriter(filePath, true));
            }
        } catch (final IOException e) {
            log.warn("IOUtils[path={}, mode={}] init error", filePath, mode, e);
        }
    }

    public String readLine() {
        String result = null;
        try {
            result = this.reader.readLine();
        } catch (final IOException e) {
            log.warn("readLine  error", e);
        }
        return result;
    }

    public void writeLine(final String line) {
        try {
            this.writer.write(line);
            this.writer.newLine();
        } catch (final IOException e) {
            log.warn("writeLine  error", e);
        }
    }

    public void close() {
        try {
            if (this.reader != null) {
                this.reader.close();
            }
            if (this.writer != null) {
                this.writer.close();
            }
        } catch (final IOException e) {
            log.warn("close  error", e);
        }
    }

    public static void writeOutputTextLine(final String prompt, final String text) {
        log.info("{}: {}", prompt, text);
    }

    public static String readInputTextLine() {
        return readInputTextLine(null);
    }

    public static String readInputTextLine(final String prompt) {
        if (prompt != null) {
            log.info("{}: ", prompt);
        }
        final BufferedReader lineOfText = new BufferedReader(new InputStreamReader(System.in));
        String textLine = null;
        try {
            textLine = lineOfText.readLine();
        } catch (final IOException e) {
            log.error("Error: ", e);
        }
        return textLine;
    }

    public static File[] listFiles(final File dir) {
        return dir.listFiles();
    }

    public static String system(final String evaluatedContents, final String failedString) {
        final Runtime runtime = Runtime.getRuntime();
        if (log.isDebugEnabled()) {
            log.debug("System = {}", evaluatedContents);
        }
        try {
            final Process process = runtime.exec(evaluatedContents);
            try (InputStreamReader reader = new InputStreamReader(process.getInputStream())) {
                final BufferedReader buffer = new BufferedReader(reader);
                final StringBuilder result = new StringBuilder();
                String data = "";
                while ((data = buffer.readLine()) != null) {
                    result.append(data).append("\n");
                }
                if (log.isDebugEnabled()) {
                    log.debug("Result = {}", failedString);
                }
                return result.toString();
            }
        } catch (final Exception e) {
            log.error("system command execution failed", e);
            return failedString;
        }
    }

    public static String evalScript(final String engineName, final String script) throws Exception {
        if (log.isDebugEnabled()) {
            log.info("Evaluating script = {}", script);
        }
        final ScriptEngineManager mgr = new ScriptEngineManager();
        final ScriptEngine engine = mgr.getEngineByName(engineName);
        return "" + engine.eval(script);
    }
}

