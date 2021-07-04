package io.github.erdos.stencil.functions;

import org.junit.Assert;
import org.junit.Test;

import java.util.IllegalFormatException;

import static io.github.erdos.stencil.functions.StringFunctions.JOIN;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;

public class StringFunctionsTest {

    @Test
    public void testJoin() {
        assertEquals("", JOIN.call(emptyList()));

        assertEquals("123", JOIN.call(asList(1, 2, 3)));
        assertEquals("123", JOIN.call(asList(1, 2, 3), null));
        assertEquals("1,2,3", JOIN.call(asList(1, 2, 3), ","));

        assertEquals("", JOIN.call(emptyList(), null));

        try {
            assertEquals("", JOIN.call(null, ","));
            Assert.fail("Expected exception!");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }
}