package io.github.erdos.stencil.functions;

import org.junit.Test;

import static io.github.erdos.stencil.functions.BasicFunctions.COALESCE;
import static io.github.erdos.stencil.functions.BasicFunctions.EMPTY;
import static io.github.erdos.stencil.functions.BasicFunctions.SWITCH;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.*;

public class BasicFunctionsTest {

    @Test
    public void testSwitch() {
        assertEquals(3, SWITCH.call("a", "b", 1, "c", 2, "a", 3));
        assertEquals(3, SWITCH.call("a", "b", 1, "c", 2, "a", 3, "default"));
        assertEquals(1, SWITCH.call("a", "a", 1));
        assertEquals(555, SWITCH.call(null, "b", 1, null, 555, "a", 3, "default"));

        assertNull(SWITCH.call("a", "x", 1, "y", 2));
    }

    @Test
    public void testCoalesce() {
        assertEquals(3, COALESCE.call("", null, emptyList(), 3, emptyList()));
        assertEquals(3, COALESCE.call(3, null, null, emptyList()));

        assertNull(COALESCE.call());
        assertNull(COALESCE.call(null, emptyList(), "", null, emptyList()));
    }

    @Test
    public void testEmpty() {
        assertTrue((Boolean) EMPTY.call(""));
        assertTrue((Boolean) EMPTY.call(emptyList()));
        assertTrue((Boolean) EMPTY.call((Object) null));

        assertFalse((Boolean) EMPTY.call("aasd"));
        assertFalse((Boolean) EMPTY.call(asList(1, 2, 3)));
        assertFalse((Boolean) EMPTY.call(234));
    }
}