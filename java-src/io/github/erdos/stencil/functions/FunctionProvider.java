package io.github.erdos.stencil.functions;

import java.util.Collection;

public interface FunctionProvider {


    int DEFAULT_PRIORITY = 10;

    /**
     * Return the functions instances for the current render.
     * The provider may choose to return new instances of a function for each call, if the function is not pure:
     * e.g.: a counter, which returns an increasing sequence of numbers for each call.
     *
     * @return the functions provided by this provider
     */
    Collection<Function> functions();

    /**
     * Priority of the provider.
     * <p>
     * Providers are called in ascending order of priority.
     * <p>
     * Default priority is 10.
     * NB: Multimethod functions defined in Clojure namespaces have priority over Java defined functions.
     *
     * @return priority of the provider
     */
    default int priority() {
        return DEFAULT_PRIORITY;
    }
}
