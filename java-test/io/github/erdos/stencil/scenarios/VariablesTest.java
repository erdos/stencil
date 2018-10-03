package io.github.erdos.stencil.scenarios;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static java.util.Arrays.asList;
import static io.github.erdos.stencil.scenarios.TableColumnsTest.testWithData;

/**
 * Test IF-THEN, IF-THEN-ELSE, UNLESS-THEN, UNLESS-THEN-ELSE statements
 */
public class VariablesTest {

    /**
     * First branches are executed in every conditional expression.
     */
    @Test
    public void testConditionals1() {
        Map<String, Object> data = new HashMap<>();

        data.put("condition1", true);
        data.put("condition2", true);
        data.put("condition3", false);
        data.put("condition4", false);

        testWithData("test-control-conditionals.docx",
                data,
                asList("Apple", "Banana", "Date", "Elderberry"),
                asList("Cherry", "Fig"));
    }

    /**
     * Second branches are executed in every conditional expression.
     */
    @Test
    public void testConditionals2() {
        Map<String, Object> data = new HashMap<>();

        data.put("condition1", false);
        data.put("condition2", false);
        data.put("condition3", true);
        data.put("condition4", true);

        testWithData("test-control-conditionals.docx",
                data,
                asList("Cherry", "Fig"),
                asList("Apple", "Banana", "Date", "Elderberry"));
    }
}
