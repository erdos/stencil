package io.github.erdos.stencil.standalone;

import java.util.*;
import java.util.function.Function;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;

public class ArgsParser {

    private final Set<ParamMarker> markers = new HashSet<>();

    public <T> ParamMarker<T> addParam(char shortForm, String longForm, String description, Function<String, T> parser) {
        final ParamMarker<T> added = new ParamMarker<>(parser, shortForm, longForm, description);
        markers.add(added);
        return added;
    }

    public ParseResult parse(String... args) {


        final Map<Character, String> result = new HashMap<>();
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

                    if (item.startsWith("--")) {
                        // long form
                    } else {
                        // short form
                    }
                    // TODO: do parsing here!

                } else {
                    // maybe the next item is a value
                    final String nextItem = i + 1 < args.length ? args[i + 1] : null;
                    if (nextItem == null || nextItem.startsWith("-")) {
                        // if next item is missing or is not a corrent value
                        // then we should throw an exception here!
                        // or if arg is a flag then we can consider this step as setting it to true/false
                        throw new IllegalArgumentException("No value found for argument " + item);
                    } else {
                        // TODO: just parse next item
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

    private static final Set<String> YES = Collections.unmodifiableSet(new HashSet<>(asList("yes", "y", "true", "t", "1")));
    private static final Set<String> NO = Collections.unmodifiableSet(new HashSet<>(asList("no", "n", "false", "f", "0")));

    public ParamMarker<Boolean> addFlagOption(char shortName, String longName, String description) {
        return addParam(shortName, longName, description, x -> {
            if (YES.contains(x.toLowerCase())) {
                return true;
            } else if (NO.contains(x.toLowerCase())) {
                return false;
            } else {
                throw new IllegalArgumentException("Could not parse argument value as boolean: " + x);
            }
        });
    }

    public static final class ParseResult {
        private final Map<Character, String> args;
        private final List<String> varargs;

        private ParseResult(Map<Character, String> args, List<String> varargs) {
            this.args = unmodifiableMap(args);
            this.varargs = unmodifiableList(varargs);
        }


        public List<String> getRestArgs() {
            return Collections.unmodifiableList(varargs);
        }


        public <T> Optional<T> getParamValue(ParamMarker<T> marker) {
            final char shortName = marker.getShortForm();
            if (args.containsKey(shortName)) {
                final String argValue = args.get(shortName);
                return Optional.of(marker.parse(argValue));
            } else {
                return Optional.empty();
            }
        }
    }

    private static final class ParamMarker<T> {
        private final Function<String, T> parse;
        private final char shortForm;
        private final String longName;
        private final String description;

        private ParamMarker(Function<String, T> parse, char shortForm, String longName, String description) {
            this.parse = parse;
            this.shortForm = shortForm;
            this.longName = longName;
            this.description = description;
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
    }
}
