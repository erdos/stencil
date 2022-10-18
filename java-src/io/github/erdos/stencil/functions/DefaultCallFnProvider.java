package io.github.erdos.stencil.functions;

public class DefaultCallFnProvider extends ClojureCallFnProvider {
    /**
     * The built-in call-fn functions are defined in the stencil.functions namespace.
     * This constructor loads it.
     */
    public DefaultCallFnProvider() {
        super("stencil.functions");
    }
}
