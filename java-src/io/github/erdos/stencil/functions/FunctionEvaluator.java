package io.github.erdos.stencil.functions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public final class FunctionEvaluator {

    private final Map<String, Function> functions = new HashMap<>();

    {
        registerFunctions(BasicFunctions.values());
        registerFunctions(StringFunctions.values());
        registerFunctions(NumberFunctions.values());
        registerFunctions(DateFunctions.values());
        registerFunctions(LocaleFunctions.values());
    }

    private void registerFunction(Function function) {
        if (function == null)
            throw new IllegalArgumentException("Registered function must not be null.");
        functions.put(function.getName().toLowerCase(), function);
    }

    /**
     * Registers a function to this evaluator engine.
     * Registered functions can be invoked from inside template files.
     *
     * @param functions any number of function instances.
     */
    @SuppressWarnings("WeakerAccess")
    public void registerFunctions(Function... functions) {
        for (Function function : functions) {
            registerFunction(function);
        }
    }

    /**
     * Calls a function by name.
     *
     * @param functionName Case Insensitive name of fn to call.
     * @param arguments    arguments dispatched to called function
     * @return result of function call
     * @throws IllegalArgumentException when function name is null or missing
     */
    public Object call(String functionName, Object... arguments) {
        if (functionName == null)
            throw new IllegalArgumentException("Function name is missing");
        final Function fun = functions.get(functionName.toLowerCase());
        if (fun == null)
            throw new IllegalArgumentException("Did not find function for name " + functionName);
        return fun.call(arguments);
    }

    /**
     * Returns a thread-safe sequence of all registered functions.
     */
    @SuppressWarnings("WeakerAccess")
    public Iterable<Function> listFunctions() {
        return new ArrayList<>(functions.values());
    }
}
