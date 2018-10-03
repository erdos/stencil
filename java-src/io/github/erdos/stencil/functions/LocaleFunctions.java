package io.github.erdos.stencil.functions;

import java.text.NumberFormat;
import java.util.Locale;

import static java.util.Locale.forLanguageTag;
import static java.util.Locale.getDefault;

public enum LocaleFunctions implements Function {


    /**
     * Formats number as localized currency. An optional second argument can be used to specify locale code.
     * Returns a string.
     * <p>
     * Usage: currency(34) or currency(34, "HU")
     */
    CURRENCY {
        @Override
        public Object call(Object... arguments) throws IllegalArgumentException {
            return formatting(this, NumberFormat::getCurrencyInstance, arguments);
        }
    },

    /**
     * Formats number as localized percent. An optional second argument can be used to specify locale code.
     * Returns a string.
     * <p>
     * Usage: percent(34) or percent(34, "HU")
     */
    PERCENT {
        @Override
        public Object call(Object... arguments) throws IllegalArgumentException {
            return formatting(this, NumberFormat::getPercentInstance, arguments);
        }
    };

    private static String formatting(Function function, java.util.function.Function<Locale, NumberFormat> fun, Object... arguments) {
        if (arguments.length == 0 || arguments.length > 2) {
            throw new IllegalArgumentException(function.getName() + "() function expects 1 or 2 arguments");
        } else if (arguments[0] == null) {
            return null;
        } else {

            final Object value = arguments[0];
            final Locale locale = (arguments.length == 2) ? forLanguageTag(arguments[1].toString()) : getDefault();

            return fun.apply(locale).format(value);
        }
    }

    @Override
    public String getName() {
        return name().toLowerCase();
    }
}
