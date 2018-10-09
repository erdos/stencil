package io.github.erdos.stencil.functions;

/**
 * Common numeric functions.
 */
@SuppressWarnings("unused")
public enum NumberFunctions implements Function {

    /**
     * Rounds a number to the nearest integer.
     * <p>
     * Expects 1 argument and returns null on null argument.
     */
    ROUND {
        @Override
        public Object call(Object... arguments) {
            final Number n = NumberFunctions.maybeNumber(arguments);
            if (n == null) {
                return null;
            } else {
                return Math.round(n.doubleValue());
            }
        }
    },

    /**
     * Rounds a number to the closest bigger integer value.
     * <p>
     * Expects 1 argument and returns null on null argument.
     */
    CEIL {
        @Override
        public Object call(Object... arguments) {
            final Number n = NumberFunctions.maybeNumber(arguments);
            if (n == null) {
                return null;
            } else {
                return (long) Math.ceil(n.doubleValue());
            }
        }
    },

    /**
     * Rounds a number to the closest smaller integer value.
     * <p>
     * Expects 1 argument and returns null on null argument.
     */
    FLOOR {
        @Override
        public Object call(Object... arguments) {
            final Number n = NumberFunctions.maybeNumber(arguments);
            if (n == null) {
                return null;
            } else {
                return (long) Math.floor(n.doubleValue());
            }
        }
    };

    @Override
    public String getName() {
        return name().toLowerCase();
    }

    private static Number maybeNumber(Object... arguments) {
        if (arguments.length != 1) {
            throw new IllegalArgumentException("The function must have exactly 1 arguments!");
        } else if (arguments[0] == null) {
            return null;
        } else if (!(arguments[0] instanceof Number)) {
            throw new IllegalArgumentException("The function expects a number argument!");
        } else {
            return (Number) arguments[0];
        }
    }
}
