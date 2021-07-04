package io.github.erdos.stencil.exceptions;

/**
 * Indicates that an error happened during the evaluation of a stencil expression in a template.
 */
public final class EvalException extends RuntimeException {

    private EvalException(Exception cause) {
        super(cause);
    }

    private EvalException(String message) {
        super(message);
    }

    public static EvalException fromMissingValue(String expression) {
        return new EvalException("Value is missing for expression: " + expression);
    }

    public static EvalException wrapping(Exception cause) {
        return new EvalException(cause);
    }
}

