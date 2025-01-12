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
package org.goldrenard.jb.i18n;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.goldrenard.jb.core.Bot;
import org.goldrenard.jb.model.AIMLMap;
import org.goldrenard.jb.model.AIMLSet;
import org.goldrenard.jb.utils.Utilities;

public class Verbs {
    private static Set<String> es = Utilities.stringSet("sh", "ch", "th", "ss", "x");
    private static Set<String> ies = Utilities.stringSet("ly", "ry", "ny", "fy", "dy", "py");
    private static Set<String> ring = Utilities.stringSet("be", "me", "re", "se", "ve", "de", "le", "ce", "ze", "ke", "te", "ge", "ne", "pe", "ue");
    private static Set<String> bing = Utilities.stringSet("ab", "at", "op", "el", "in", "ur", "op", "er", "un", "in", "it", "et", "ut", "im", "id", "ol", "ig");
    private static Set<String> notBing = Utilities.stringSet("der", "eat", "ber", "ain", "sit", "ait", "uit", "eet", "ter", "lop", "ver", "wer", "aim", "oid", "eel", "out", "oin", "fer", "vel", "mit");

    public static Set<String> irregular = new HashSet<>();
    private static Map<String, String> be2was = new HashMap<>();
    private static Map<String, String> be2been = new HashMap<>();
    private static Map<String, String> be2is = new HashMap<>();
    private static Map<String, String> be2being = new HashMap<>();
    private static Set<String> allVerbs = new HashSet<>();

    public static String endsWith(final String verb, final Set<String> endings) {
        for (final String x : endings) {
            if (verb.endsWith(x)) {
                return x;
            }
        }
        return null;
    }

    public static String is(final String verb) {
        String ending;
        if (irregular.contains(verb)) {
            return be2is.get(verb);
        }
        if (verb.endsWith("go") || ((ending = endsWith(verb, es)) != null)) {
            return verb + "es";
        }
        if ((ending = endsWith(verb, ies)) != null) {
            return verb.substring(0, verb.length() - 1) + "ies";
        }
        return verb + "s";
    }

    public static String was(String verb) {
        String ending;
        verb = verb.trim();
        if (verb.equals("admit")) {
            return "admitted";
        }
        if (verb.equals("commit")) {
            return "committed";
        }
        if (verb.equals("die")) {
            return "died";
        }
        if (verb.equals("agree")) {
            return "agreed";
        }
        if (verb.endsWith("efer")) {
            return verb + "red";
        }

        if (irregular.contains(verb)) {
            return be2was.get(verb);
        }
        if ((ending = endsWith(verb, ies)) != null) {
            return verb.substring(0, verb.length() - 1) + "ied";
        }
        if ((ending = endsWith(verb, ring)) != null) {
            return verb + "d";
        }
        if ((ending = endsWith(verb, bing)) != null && (null == endsWith(verb, notBing))) {
            return verb + ending.substring(1, 2) + "ed";
        }
        return verb + "ed";
    }

    public static String being(final String verb) {
        String ending;
        if (irregular.contains(verb)) {
            return be2being.get(verb);
        }
        if (verb.equals("admit")) {
            return "admitting";
        }
        if (verb.equals("commit")) {
            return "committing";
        }
        if (verb.equals("quit")) {
            return "quitting";
        }
        if (verb.equals("die")) {
            return "dying";
        }
        if (verb.equals("lie")) {
            return "lying";
        }
        if (verb.endsWith("efer")) {
            return verb + "ring";
        }
        if ((ending = endsWith(verb, ring)) != null) {
            return verb.substring(0, verb.length() - 1) + "ing";
        }
        if ((ending = endsWith(verb, bing)) != null && (null == endsWith(verb, notBing))) {
            return verb + ending.substring(1, 2) + "ing";
        }
        return verb + "ing";
    }

    public static String been(final String verb) {
        if (irregular.contains(verb)) {
            return (be2been.get(verb));
        }
        return was(verb);
    }

    public static void getIrregulars() {
        // Do, Did, Done, Does, Doing
        // be, was, been, is, being

        for (String x : Utilities.readFileLines("c:/ab/data/irrverbs.txt")) {
            x = x.toLowerCase();
            final String[] triple = x.split(",");
            if (triple.length == 5) {
                irregular.add(triple[0]);
                allVerbs.add(triple[0]);
                be2was.put(triple[0], triple[1]);
                be2been.put(triple[0], triple[2]);
                be2is.put(triple[0], triple[3]);
                be2being.put(triple[0], triple[4]);
            }
        }
    }

    public static void makeVerbSetsMaps(final Bot bot) {
        getIrregulars();
        allVerbs.addAll(Utilities.readFileLines("c:/ab/data/verb300.txt"));
        final AIMLSet be = new AIMLSet("be", bot);
        final AIMLSet is = new AIMLSet("is", bot);
        final AIMLSet was = new AIMLSet("was", bot);
        final AIMLSet been = new AIMLSet("been", bot);
        final AIMLSet being = new AIMLSet("being", bot);
        final AIMLMap is2be = new AIMLMap("is2be", bot);
        final AIMLMap be2is = new AIMLMap("be2is", bot);
        final AIMLMap was2be = new AIMLMap("was2be", bot);
        final AIMLMap be2was = new AIMLMap("be2was", bot);
        final AIMLMap been2be = new AIMLMap("been2be", bot);
        final AIMLMap be2been = new AIMLMap("be2been", bot);
        final AIMLMap be2being = new AIMLMap("be2being", bot);
        final AIMLMap being2be = new AIMLMap("being2be", bot);

        for (final String verb : allVerbs) {
            final String beForm = verb;
            final String isForm = is(verb);
            final String wasForm = was(verb);
            final String beenForm = been(verb);
            final String beingForm = being(verb);
            be.add(beForm);
            is.add(isForm);
            was.add(wasForm);
            been.add(beenForm);
            being.add(beingForm);
            be2is.put(beForm, isForm);
            is2be.put(isForm, beForm);
            be2was.put(beForm, wasForm);
            was2be.put(wasForm, beForm);
            be2been.put(beForm, beenForm);
            been2be.put(beenForm, beForm);
            be2being.put(beForm, beingForm);
            being2be.put(beingForm, beForm);
        }
        bot.getSets().write(be);
        bot.getSets().write(is);
        bot.getSets().write(was);
        bot.getSets().write(been);
        bot.getSets().write(being);

        bot.getMaps().write(be2is);
        bot.getMaps().write(is2be);
        bot.getMaps().write(be2was);
        bot.getMaps().write(was2be);
        bot.getMaps().write(be2been);
        bot.getMaps().write(been2be);
        bot.getMaps().write(be2being);
        bot.getMaps().write(being2be);
    }
}
