package io.github.erdos.stencil.functions;

/**
 * Common numeric functions.
 */
@SuppressWarnings("unused")
public enum NumberFunctions implements Function {

    /**
     * Rounds a number to the closest integer.
     * <p>
     * Expects 1 argument and returns null on null argument.
     */
    ROUND {
        @Override
        public Object call(Object... arguments) {
            if (arguments.length != 1)
                throw new IllegalArgumentException("The round() function must have exactly 1 arguments!");
            else if (arguments[0] == null)
                return null;
            else if (!(arguments[0] instanceof Number))
                throw new IllegalArgumentException("The round() function expects a number argument!");
            else
                return Math.round(((Number) arguments[0]).doubleValue());
        }
    };

    @Override
    public String getName() {
        return name().toLowerCase();
    }
}
