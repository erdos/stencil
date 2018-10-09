package io.github.erdos.stencil.functions;

import org.junit.Test;

import static io.github.erdos.stencil.functions.NumberFunctions.ROUND;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class NumberFunctionsTest {

    @Test
    public void testRound() {

        assertNull(ROUND.call((Object) null));

        assertEquals(1L, ROUND.call(0.9));
        assertEquals(1L, ROUND.call(1.1));
        assertEquals(1L, ROUND.call(1.0));
    }
}