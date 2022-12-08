package io.github.erdos.stencil.exceptions;

/**
 * Indicates that an error happened during the evaluation of a stencil expression in a template.
 */
public final class EvalException extends RuntimeException {

    public EvalException(String message, Exception cause) {
        super(message, cause);
    }

    private EvalException(Exception cause) {
        super(cause);
    }

    private EvalException(String message) {
        super(message);
    }
}
