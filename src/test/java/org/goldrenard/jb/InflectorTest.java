package org.goldrenard.jb;

import org.goldrenard.jb.i18n.Inflector;
import org.junit.Test;

import junit.framework.TestCase;

/**
 * Created by User on 3/31/14.
 */
public class InflectorTest extends TestCase {
    @Test
    public void testPluralize() throws Exception {
        final Inflector inflector = new Inflector();
        final String pairs[][] = {{"dog", "dogs"}, {"person", "people"}, {"cats", "cats"}};
        for (final String[] pair : pairs) {
            final String singular = pair[0];
            final String expected = pair[1];
            final String actual = inflector.pluralize(singular);
            assertEquals("Pluralize " + pairs[0][0], expected, actual);
        }
    }
}
