package io.github.erdos.stencil.functions;

import org.junit.Test;

import static io.github.erdos.stencil.functions.NumberFunctions.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class NumberFunctionsTest {

    @Test
    public void testRound() {
        assertNull(ROUND.call((Object) null));

        assertEquals(1L, ROUND.call(0.9));
        assertEquals(1L, ROUND.call(1.1));
        assertEquals(2L, ROUND.call(1.5));
        assertEquals(1L, ROUND.call(1.0));

        assertEquals(-1L, ROUND.call(-0.9));
        assertEquals(-1L, ROUND.call(-1.1));
        assertEquals(-1L, ROUND.call(-1.5));
        assertEquals(-1L, ROUND.call(-1.0));
    }

    @Test
    public void testCeil() {
        assertNull(CEIL.call((Object) null));

        assertEquals(1L, CEIL.call(0.9));
        assertEquals(2L, CEIL.call(1.1));
        assertEquals(1L, CEIL.call(1.0));

        assertEquals(-0L, CEIL.call(-0.9));
        assertEquals(-1L, CEIL.call(-1.1));
        assertEquals(-1L, CEIL.call(-1.0));
    }

    @Test
    public void testFloor() {
        assertNull(FLOOR.call((Object) null));

        assertEquals(0L, FLOOR.call(0.9));
        assertEquals(1L, FLOOR.call(1.1));
        assertEquals(1L, FLOOR.call(1.0));

        assertEquals(-1L, FLOOR.call(-0.9));
        assertEquals(-2L, FLOOR.call(-1.1));
        assertEquals(-1L, FLOOR.call(-1.0));
    }
}