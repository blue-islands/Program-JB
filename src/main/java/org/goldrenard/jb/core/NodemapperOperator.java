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
package org.goldrenard.jb.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.goldrenard.jb.model.Nodemapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodemapperOperator {

    private static final Logger log = LoggerFactory.getLogger(NodemapperOperator.class);

    /**
     * number of branches from node
     *
     * @param node Nodemapper object
     * @return number of branches
     */
    public static int size(final Nodemapper node) {
        final Set<String> set = new HashSet<>();
        if (node.isShortCut()) {
            set.add("<THAT>");
        }
        if (node.getKey() != null) {
            set.add(node.getKey());
        }
        if (node.getMap() != null) {
            set.addAll(node.getMap().keySet());
        }
        return set.size();
    }

    /**
     * insert a new link from this node to another, by adding a key, value pair
     *
     * @param node  Nodemapper object
     * @param key   key word
     * @param value word maps to this next node
     */
    public static void put(final Nodemapper node, final String key, final Nodemapper value) {
        if (node.getMap() != null) {
            node.getMap().put(key, value);
        } else { // node.type == unary_node_mapper
            node.setKey(key);
            node.setValue(value);
        }
    }

    /**
     * get the node linked to this one by the word key
     *
     * @param node Nodemapper object
     * @param key  key word to map
     * @return the mapped node or null if the key is not found
     */
    public static Nodemapper get(final Nodemapper node, final String key) {
        if (node.getMap() != null) {
            return node.getMap().get(key);
        } else {// node.type == unary_node_mapper
            if (key.equals(node.getKey())) {
                return node.getValue();
            }
            return null;
        }
    }

    /**
     * check whether a node contains a particular key
     *
     * @param node Nodemapper object
     * @param key  key to test
     * @return true or false
     */
    public static boolean containsKey(final Nodemapper node, final String key) {
        if (node.getMap() != null) {
            return node.getMap().containsKey(key);
        } else {// node.type == unary_node_mapper
            return key.equals(node.getKey());
        }
    }

    /**
     * print all node keys
     *
     * @param node Nodemapper object
     */
    public static void printKeys(final Nodemapper node) {
        keySet(node).forEach(e -> log.info("{}", e));
    }

    /**
     * get key set of a node
     *
     * @param node Nodemapper object
     * @return set of keys
     */
    public static Set<String> keySet(final Nodemapper node) {
        if (node.getMap() != null) {
            return node.getMap().keySet();
        } else {// node.type == unary_node_mapper
            final Set<String> set = new HashSet<>();
            if (node.getKey() != null) {
                set.add(node.getKey());
            }
            return set;
        }
    }

    /**
     * test whether a node is a leaf
     *
     * @param node Nodemapper object
     * @return true or false
     */
    public static boolean isLeaf(final Nodemapper node) {
        return (node.getCategory() != null);
    }

    /**
     * upgrade a node from a singleton to a multi-way map
     *
     * @param node Nodemapper object
     */
    public static void upgrade(final Nodemapper node) {
        node.setMap(new HashMap<>());
        node.getMap().put(node.getKey(), node.getValue());
        node.setKey(null);
        node.setValue(null);
    }
}
