package io.github.erdos.stencil.functions;

import io.github.erdos.stencil.impl.ClojureHelper;

import java.util.Collection;
import java.util.Collections;

/**
 * This provider requires a Clojure NS which contains implementations of the call-fn multimethod.
 * The constructor defines the
 */
public abstract class ClojureCallFnProvider implements FunctionProvider {

    /**
     * The constructor loads the Clojure namespace which contains the call-fn multimethod implementations.
     * This should be called from the no-args constructor of the concrete class.
     * @param ns the clojuse namespace which contains the function implementations.
     */
    protected ClojureCallFnProvider(final String ns) {
        ClojureHelper.requireNs(ns);
    }

    @Override
    /**
     * This provider does not provide any functions directly.
     *
     * @return an empty collection
     */
    public Collection<Function> functions() {
        return Collections.emptyList();
    }

    @Override
    /*
     * call-fn implementations have priority over Java Function implementations during evaluation.
     */
    public int priority() {
        return Integer.MIN_VALUE;
    }
}
