package io.github.erdos.stencil.exceptions;

/**
 * This class indicates an error while reading and parsing a stencil expression.
 */
public final class ParsingException extends RuntimeException {

    private final String expression;

    public static ParsingException fromMessage(String expression, String message) {
        return new ParsingException(expression, message);
    }

    public static ParsingException wrapping(String message, Exception cause) {
        return new ParsingException(message, cause);
    }

    private ParsingException(String expression, String message) {
        super(message);
        this.expression = expression;
    }

    private ParsingException(String message, Exception cause) {
        super(message, cause);
        this.expression = "";
    }

    public String getExpression() {
        return expression;
    }
}
