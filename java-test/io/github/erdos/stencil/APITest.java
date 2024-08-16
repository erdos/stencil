package io.github.erdos.stencil;

import io.github.erdos.stencil.exceptions.EvalException;
import io.github.erdos.stencil.functions.Function;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class APITest {

    private static class CustomFunction implements Function {
        private int callCount = 0;
        private Object lastResult = null;

        @Override
        public Object call(final Object... arguments) throws IllegalArgumentException {
            callCount++;
            lastResult = arguments.length > 0 ? arguments[0] : arguments;
            return lastResult;
        }

        @Override
        public String getName() {
            return "customFunction";
        }
    }

    @Test
    public void testCustomFunction() throws IOException {
        final CustomFunction fn = new CustomFunction();
        try (final PreparedTemplate prepared =
                     API.prepare(new File("test-resources/test-custom-function.docx").toPath())) {
            final Map<String, Object> data = new HashMap<>();
            data.put("input", "testInput");
            API.render(prepared, Collections.emptyMap(), TemplateData.fromMap(data), Collections.singletonList(fn));
        }
        Assert.assertTrue("Custom function should have been called", fn.callCount > 0);
        Assert.assertEquals("Custom function returned unexpected value", "testInput", fn.lastResult);
    }

    @Test(expected = EvalException.class)
    public void testWithoutCustomFunction() throws IOException {
        try (final PreparedTemplate prepared =
                     API.prepare(new File("test-resources/test-custom-function.docx").toPath())) {
            final Map<String, Object> data = new HashMap<>();
            data.put("input", "testInput");
            API.render(prepared, Collections.emptyMap(), TemplateData.fromMap(data));
            Assert.fail("Should have thrown exception");
        }
    }
}
