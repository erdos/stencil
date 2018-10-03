package io.github.erdos.stencil.functions;

import org.junit.Assert;
import org.junit.Test;

import static io.github.erdos.stencil.functions.StringFunctions.FORMAT;
import static org.junit.Assert.assertEquals;

public class StringFunctionsTest {

    @Test
    public void testFormat() {
        assertEquals("", FORMAT.call(""));
        assertEquals("158", FORMAT.call("%x", 344));

        try {
            FORMAT.call("%x");
            Assert.fail("Kevetel kellene!");
        } catch (IllegalArgumentException ignored) {
            // direkt ures!
        }

        try {
            FORMAT.call();
            Assert.fail("Kevetel kellene!");
        } catch (IllegalArgumentException ignored) {
            // direkt ures!
        }
    }
}