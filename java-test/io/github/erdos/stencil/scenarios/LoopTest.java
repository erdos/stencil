package io.github.erdos.stencil.scenarios;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonMap;
import static io.github.erdos.stencil.scenarios.TableColumnsTest.testWithData;

/**
 * Tests {%for e in elems%} expressions.
 */
public class LoopTest {

    /**
     * A simple iterator is looped over values of an array.
     */
    @Test
    public void testLoop1() {
        Map<String, Object> data = new HashMap<>();

        data.put("elems",
                asList(singletonMap("value", "one"),
                        singletonMap("value", "2"),
                        singletonMap("value", "three")));

        testWithData("test-control-loop.docx",
                data,
                asList("one", "2", "three"),
                emptyList());
    }
}
