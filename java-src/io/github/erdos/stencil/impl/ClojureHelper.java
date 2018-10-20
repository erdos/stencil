package io.github.erdos.stencil.impl;

import clojure.lang.IFn;
import clojure.lang.Keyword;
import clojure.lang.RT;
import clojure.lang.Symbol;

/**
 * Clojure utilities.
 */
@SuppressWarnings("WeakerAccess")
public class ClojureHelper {

    /**
     * Clojure :stream keyword
     */
    public static final Keyword KV_STREAM = Keyword.intern("stream");

    /**
     * Clojure :variables keyword
     */
    public static final Keyword KV_VARIABLES = Keyword.intern("variables");

    /**
     * Clojure :template keyword
     */
    public static final Keyword KV_TEMPLATE = Keyword.intern("template");

    /**
     * Clojure :data keyword
     */
    public static final Keyword KV_DATA = Keyword.intern("data");

    /**
     * Clojure :format keyword
     */
    public static final Keyword KV_FORMAT = Keyword.intern("format");

    /**
     * Clojure :function keyword
     */
    public static final Keyword KV_FUNCTION = Keyword.intern("function");

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

    /**
     * Shuts down clojure agents. Needed to speed up quitting from Clojure programs.
     */
    public static void callShutdownAgents() {
        RT.var("clojure.core", "shutdown-agents").run();
    }
}

