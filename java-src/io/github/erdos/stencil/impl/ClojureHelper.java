package io.github.erdos.stencil.impl;

import clojure.lang.IFn;
import clojure.lang.Keyword;
import clojure.lang.RT;
import clojure.lang.Symbol;

/**
 * Clojure utilities.
 */
@SuppressWarnings("WeakerAccess")
public final class ClojureHelper {

    private ClojureHelper() {}

    enum Keywords {
        DATA, FUNCTION, FRAGMENTS, TEMPLATE;

        public final Keyword kw = Keyword.intern(name().toLowerCase().replace('_', '-'));
    }

    // requires stencil.process namespace so stencil is loaded.
    static {
        final IFn req = RT.var("clojure.core", "require");
        req.invoke(Symbol.intern("stencil.process"));
    }

    /**
     * Finds a function in stencil.process namespace and returns it.
     *
     * @param functionName name of var
     * @return function with for given name
     */
    public static IFn findFunction(String functionName) {
        return RT.var("stencil.process", functionName);
    }
}
