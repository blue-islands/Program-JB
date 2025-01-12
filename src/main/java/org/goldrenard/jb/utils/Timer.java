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

/**
 * Specialized timer function for program instrumentation
 */
public class Timer {
    private long startTimeMillis;

    public Timer() {
        this.start();
    }

    public void start() {
        this.startTimeMillis = System.currentTimeMillis();
    }

    public long elapsedTimeMillis() {
        return System.currentTimeMillis() - this.startTimeMillis + 1;
    }

    public long elapsedRestartMs() {
        final long ms = System.currentTimeMillis() - this.startTimeMillis + 1;
        this.start();
        return ms;
    }

    public float elapsedTimeSecs() {
        return this.elapsedTimeMillis() / 1000F;
    }

    public float elapsedTimeMins() {
        return this.elapsedTimeSecs() / 60F;
    }
}
