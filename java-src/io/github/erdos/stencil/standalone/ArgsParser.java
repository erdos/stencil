package io.github.erdos.stencil.standalone;

import java.util.*;
import java.util.function.Function;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;

@SuppressWarnings("WeakerAccess")
public class ArgsParser {

    private final Set<ParamMarker> markers = new HashSet<>();

    public <T> ParamMarker<T> addParam(char shortForm, String longForm, String description, Function<String, T> parser) {
        final ParamMarker<T> added = new ParamMarker<>(parser, shortForm, longForm, description, false);
        markers.add(added);
        return added;
    }

    public ParseResult parse(String... args) {

        final Map<ParamMarker, String> result = new HashMap<>();

        final List<String> restArgs = new ArrayList<>(args.length);

        for (int i = 0; i < args.length; i++) {
            String item = args[i];
            if ("--".equals(item)) {
                // the rest are varargs
                for (i++; i < args.length; i++) {
                    restArgs.add(args[i]);
                }
            } else if (item.startsWith("-")) {
                // TODO: parse this.
                if (item.contains("=")) {
                    // it is an equation.
                    final String[] parts = item.split("=", 2);
                    final String argName = parts[0];
                    final String argValue = parts[1];

                    final Map.Entry<ParamMarker, String> flag = parsePair(argName, argValue);

                    result.put(flag.getKey(), flag.getValue());
                } else {

                    if (item.startsWith("--no-")) {
                        // long form
                        if (markerForLong(item.substring(5)).orElseThrow(() -> new IllegalArgumentException("Unexpected option: " + item)).isFlag()) {
                            Map.Entry<ParamMarker, String> pair = parsePair(item, null);
                            result.put(pair.getKey(), pair.getValue());
                            continue;
                        }
                    } else if (item.startsWith("--")) {
                        // long form
                        if (markerForLong(item.substring(2)).orElseThrow(() -> new IllegalArgumentException("Unexpected option: " + item)).isFlag()) {
                            Map.Entry<ParamMarker, String> pair = parsePair(item, null);
                            result.put(pair.getKey(), pair.getValue());
                            continue;
                        }
                    } else {
                        // short form
                        if (markerForShort(item.charAt(1)).orElseThrow(() -> new IllegalArgumentException("Unexpected option: " + item)).isFlag()) {
                            Map.Entry<ParamMarker, String> pair = parsePair(item, null);
                            result.put(pair.getKey(), pair.getValue());
                            continue;
                        }
                    }

                    // maybe the next item is a value
                    final String nextItem = i + 1 < args.length ? args[i + 1] : null;
                    if (nextItem == null || nextItem.startsWith("-")) {
                        final Map.Entry<ParamMarker, String> flag = parsePair(item, null);
                        result.put(flag.getKey(), flag.getValue());
                    } else {
                        final Map.Entry<ParamMarker, String> flag = parsePair(item, nextItem);
                        result.put(flag.getKey(), flag.getValue());
                    }
                }
            } else {
                // the rest are varargs including current one
                for (; i < args.length; i++) {
                    restArgs.add(args[i]);
                }
            }
        }

        return new ParseResult(result, restArgs);
    }

    private Map.Entry<ParamMarker, String> parsePair(String k, String v) {
        if (k.startsWith("--no-") && v == null) {
            final String longName = k.substring(5);
            final ParamMarker marker = markerForLong(longName).get();
            return new AbstractMap.SimpleImmutableEntry<>(marker, "false");
        } else if (k.startsWith("--")) {
            final String longName = k.substring(2);
            final ParamMarker marker = markerForLong(longName).get();
            if (v == null) {
                return new AbstractMap.SimpleImmutableEntry<>(marker, "true");
            } else {
                return new AbstractMap.SimpleImmutableEntry<>(marker, v);
            }
        } else if (k.startsWith("-") && k.length() == 2 && v == null) {
            final ParamMarker marker = markerForShort(k.charAt(1)).get();
            return new AbstractMap.SimpleImmutableEntry<>(marker, "true");
        } else {
            throw new IllegalArgumentException("Unexpected key, not a parameter: " + k);
        }
    }

    private Optional<ParamMarker> markerForLong(String longName) {
        return markers.stream().filter(x -> x.getLongName().equals(longName)).findAny();
    }

    private Optional<ParamMarker> markerForShort(char shortName) {
        return markers.stream().filter(x -> x.getShortForm() == shortName).findAny();
    }


    private static final Set<String> YES = Collections.unmodifiableSet(new HashSet<>(asList("yes", "y", "true", "t", "1")));
    private static final Set<String> NO = Collections.unmodifiableSet(new HashSet<>(asList("no", "n", "false", "f", "0")));

    private static final Function<String, Boolean> PARSE_BOOLEAN = x -> {
        if (YES.contains(x.toLowerCase())) {
            return true;
        } else if (NO.contains(x.toLowerCase())) {
            return false;
        } else {
            throw new IllegalArgumentException("Could not parse argument value as boolean: " + x);
        }
    };

    public ParamMarker<Boolean> addFlagOption(char shortName, String longName, String description) {
        final ParamMarker<Boolean> added = new ParamMarker<>(PARSE_BOOLEAN, shortName, longName, description, true);
        markers.add(added);
        return added;
    }

    public static final class ParseResult {
        private final Map<ParamMarker, String> args;
        private final List<String> varargs;

        private ParseResult(Map<ParamMarker, String> args, List<String> varargs) {
            this.args = unmodifiableMap(args);
            this.varargs = unmodifiableList(varargs);
        }


        public List<String> getRestArgs() {
            return Collections.unmodifiableList(varargs);
        }


        public <T> Optional<T> getParamValue(ParamMarker<T> marker) {
            if (args.containsKey(marker)) {
                final String argValue = args.get(marker);
                return Optional.of(marker.parse(argValue));
            } else {
                return Optional.empty();
            }
        }
    }

    public static final class ParamMarker<T> {
        private final Function<String, T> parse;
        private final char shortForm;
        private final String longName;
        private final String description;
        private final boolean flag;

        private ParamMarker(Function<String, T> parse, char shortForm, String longName, String description, boolean flag) {
            this.parse = parse;
            this.shortForm = shortForm;
            this.longName = longName;
            this.description = description;
            this.flag = flag;
        }

        char getShortForm() {
            return shortForm;
        }

        T parse(String input) {
            return parse.apply(input);
        }

        public String getLongName() {
            return longName;
        }

        public String getDescription() {
            return description;
        }

        public boolean isFlag() {
            return flag;
        }
    }
}
