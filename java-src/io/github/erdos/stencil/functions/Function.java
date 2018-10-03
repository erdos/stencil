package io.github.erdos.stencil.functions;

/**
 * A function object can be called from inside a template file.
 * <p>
 * Function calls should return simple values that can be embedded in the document: numbers, bools, strings.
 */
@SuppressWarnings("unused")
public interface Function {

    /**
     * A simple function call.
     *
     * @param arguments array of arguments, never null.
     * @return a simple value to insert in the template file.
     * @throws IllegalArgumentException when argument count or argument types are unexpected.
     */
    Object call(Object... arguments) throws IllegalArgumentException;

    /**
     * Name of the function used as function identifier.
     * <p>
     * Must not contain whitespaces. Must not be empty. Must not change.
     * It is used to look up the function from the template file.
     *
     * @return function identifier
     */
    String getName();
}
