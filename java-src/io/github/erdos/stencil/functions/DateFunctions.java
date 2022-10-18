package io.github.erdos.stencil.functions;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

import static java.util.Arrays.asList;
import static java.util.Locale.forLanguageTag;
import static java.util.Locale.getDefault;
import static java.util.Optional.empty;
import static java.util.Optional.of;

/**
 * Date handling functions
 */
public enum DateFunctions implements Function {


    /**
     * Formats a date object.
     * <p>
     * First argument is date format string. Second argument is some date value (either some object or string).
     * <p>
     * Example call: date("yyyy-MM-dd", x.birthDate)
     */
    DATE {
        @Override
        public Object call(Object... arguments) throws IllegalArgumentException {
            if (arguments.length != 2 && arguments.length != 3)
                throw new IllegalArgumentException("date() function expects exactly 2 or 3 arguments!");
            if (arguments[0] == null || arguments[1] == null || arguments[1].toString().isEmpty())
                return null;

            final Locale locale;
            final String pattern;
            final Object datum;

            if (arguments.length == 2) {
                locale = getDefault();
                pattern = arguments[0].toString();
                datum = arguments[1];
            } else {
                if (arguments[2] == null || arguments[2].toString().isEmpty()) {
                    return null;
                }
                locale = forLanguageTag(arguments[0].toString());
                pattern = arguments[1].toString();
                datum = arguments[2];
            }

            final Optional<LocalDate> d2 = DateFunctions.maybeLocalDate(datum);
            if (d2.isPresent()) {
                return d2.get().format(DateTimeFormatter.ofPattern(pattern, locale));
            }

            final Optional<LocalDateTime> d3 = DateFunctions.maybeLocalDateTime(datum);
            if (d3.isPresent()) {
                return d3.get().format(DateTimeFormatter.ofPattern(pattern, locale));
            }

            final Optional<Date> d1 = DateFunctions.maybeDate(datum);
            if (d1.isPresent()) {
                return new SimpleDateFormat(pattern, locale).format(d1.get());
            }

            throw new IllegalArgumentException("Could not parse date object " + datum.toString());
        }
    };

    // simple datetime format
    public final static String DATETIME1 = "yyyy-MM-dd HH:mm:ss";

    // new Date().toString() gives this format
    public static final String DATE_TOSTRING = "EEE MMM dd HH:mm:ss Z yyyy";


    // new Date().toString() gives this format
    public static final String DATE_DOTTED = "yyyy. MM. dd.";


    // standard formats
    public final static String RFC1123 = "EEE, dd MMM yyyy HH:mm:ss zzz";
    public final static String RFC1036 = "EEEE, dd-MMM-yy HH:mm:ss zzz";
    public final static String ASCTIME = "EEE MMM d HH:mm:ss yyyy";
    public final static String ISO8601 = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";

    public final static List<String> PATTERNS = asList(DATETIME1, RFC1036, RFC1123, ASCTIME, ISO8601, DATE_TOSTRING, DATE_DOTTED);

    private static Optional<Date> maybeDate(Object obj) {
        if (obj instanceof Date)
            return of((Date) obj);
        String s = obj.toString();
        try {
            return of(DatatypeFactory.newInstance().newXMLGregorianCalendar(s).toGregorianCalendar().getTime());
        } catch (IllegalArgumentException | DatatypeConfigurationException e) {
            for (String p : PATTERNS) {
                try {
                    return of(new SimpleDateFormat(p).parse(s));
                } catch (ParseException ignored) {
                }
            }
            return empty();
        }
    }

    private static Optional<LocalDate> maybeLocalDate(Object obj) {
        if (obj instanceof LocalDate)
            return of((LocalDate) obj);
        try {
            return of(LocalDate.parse(obj.toString()));
        } catch (DateTimeParseException e) {
            return empty();
        }
    }

    private static Optional<LocalDateTime> maybeLocalDateTime(Object obj) {
        if (obj instanceof LocalDateTime)
            return of((LocalDateTime) obj);
        try {
            return of(LocalDateTime.parse(obj.toString()));
        } catch (DateTimeParseException e) {
            return empty();
        }
    }

    @Override
    public String getName() {
        return name().toLowerCase();
    }

    public static class Provider implements FunctionProvider {

        private static final List<Function> FUNCTIONS = Arrays.asList(values());

        @Override
        public Collection<Function> functions() {
            return FUNCTIONS;
        }
    }
}
