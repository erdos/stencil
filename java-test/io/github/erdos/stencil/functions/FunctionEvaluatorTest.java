package io.github.erdos.stencil.functions;

import org.junit.Test;

import static io.github.erdos.stencil.functions.BasicFunctions.EMPTY;
import static org.junit.Assert.assertTrue;

public class FunctionEvaluatorTest {
    
    @Test
    public void allFunctionsMustBeNullSafe() {
        for (Function fun : new FunctionEvaluator().listFunctions()) {
            if (fun == EMPTY) continue;
            try {
                Object result = fun.call((Object) null);
                assertTrue("Not for " + fun, result == null || result.equals(""));
            } catch (IllegalArgumentException ignored) {
                // ok
            }
        }
    }
}