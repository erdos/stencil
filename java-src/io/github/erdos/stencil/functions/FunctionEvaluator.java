package io.github.erdos.stencil.functions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

public final class FunctionEvaluator {

    private final Map<String, Function> functions = new HashMap<>();

    private static final ServiceLoader<FunctionsProvider> providers = ServiceLoader.load(FunctionsProvider.class);

    {
        for (FunctionsProvider provider : providers) {
            registerFunctions(provider);
        }
    }

    private void registerFunction(Function function) {
        if (function == null)
            throw new IllegalArgumentException("Registered function must not be null.");
        Function present = functions.put(function.getName().toLowerCase(), function);
        if (present != null)
            throw new IllegalArgumentException("Function with name has already been registered.");
    }

    /**
     * Registers a function to this evaluator engine.
     * Registered functions can be invoked from inside template files.
     *
     * @param provider contains list of functions to register
     */

    @SuppressWarnings("WeakerAccess")
    public void registerFunctions(FunctionsProvider provider) {
        for (Function function : provider.functions()) {
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
