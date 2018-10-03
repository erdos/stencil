package io.github.erdos.stencil.functions;

import java.util.Arrays;
import java.util.IllegalFormatException;
import java.util.stream.Collectors;

/**
 * Common string functions.
 */
@SuppressWarnings("unused")
public enum StringFunctions implements Function {

    /**
     * Calls the standard Java String.format function.
     * <p>
     * Passes every argument to String.format.
     */
    FORMAT {
        @Override
        public Object call(Object... arguments) {
            if (arguments.length == 0) {
                throw new IllegalArgumentException("At least one arg is expected!");
            } else if (arguments[0] == null || !(arguments[0] instanceof String)) {
                throw new IllegalArgumentException("Unexpected first arg must be string!");
            } else {
                try {
                    return String.format((String) arguments[0], Arrays.copyOfRange(arguments, 1, arguments.length));
                } catch (ClassCastException | IllegalFormatException e) {
                    throw new IllegalArgumentException(e);
                }
            }
        }
    },

    /**
     * Converts parameters to strings and concatenates the result.
     * Returns empty string on empty or null arguments.
     */
    STR {
        @Override
        public Object call(Object... arguments) throws IllegalArgumentException {
            StringBuilder builder = new StringBuilder();
            for (Object argument : arguments) {
                if (argument != null)
                    builder.append(argument.toString());
            }
            return builder.toString();
        }
    },

    /**
     * Returns lowercase string of input. Expects 1 argument. Returns null on null input.
     */
    LOWERCASE {
        @Override
        public Object call(Object... arguments) throws IllegalArgumentException {
            if (arguments.length != 1)
                throw new IllegalArgumentException("lowerCase() function expects exactly 1 argument!");
            if (arguments[0] == null)
                return null;
            return arguments[0].toString().toLowerCase();
        }
    },

    /**
     * Returns UPPERCASE string of input. Expects 1 argument. Returns null on null input.
     */
    UPPERCASE {
        @Override
        public Object call(Object... arguments) throws IllegalArgumentException {
            if (arguments.length != 1)
                throw new IllegalArgumentException("upperCase() function expects exactly 1 argument!");
            if (arguments[0] == null)
                return null;
            return arguments[0].toString().toUpperCase();
        }
    },

    /**
     * Converts Every First Letter Of Every Word To Upper Case.
     * Expects 1 argument. Returns null on null input.
     */
    TITLECASE {
        @Override
        public Object call(Object... arguments) throws IllegalArgumentException {
            if (arguments.length != 1)
                throw new IllegalArgumentException("upperCase() function expects exactly 1 argument!");
            if (arguments[0] == null)
                return null;
            return Arrays.stream(arguments[0].toString().split("\\s"))
                    .map(x -> x.substring(0, 1).toUpperCase() + x.substring(1).toLowerCase())
                    .collect(Collectors.joining(" "));
        }
    };

    @Override
    public String getName() {
        return name().toLowerCase();
    }
}
