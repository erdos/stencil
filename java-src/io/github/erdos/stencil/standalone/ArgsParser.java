package io.github.erdos.stencil.standalone;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;

public class ArgsParser {

    /**
     * Represents a vararg argument list
     */
    public final static Param REST = new Param();

    public <T> ParamMarker<T> addParam(char shortForm, String longForm, String description, Function<String, T> parser) {

    }

    public ParseResult parse(String... args) {

        return null;
    }

    public void addFlagOption(char shortName, String longName, String description, boolean defaultValue) {

    }


    public void printArgs() {

    }


    public void

    public final class ParseResult {
        final Map<Param, String> args;
        final List<String> varargs;

        private ParseResult(Map<Param, String> args, List<String> varargs) {
            this.args = unmodifiableMap(args);
            this.varargs = unmodifiableList(varargs);
        }


        public String[] getRestArgs() {

        }

        /**
         * Retrieves a flag value
         *
         * @throws IllegalStateException if flag has not been configured
         */
        public boolean checkFlag() throws IllegalStateException {

        }

        public Optional<String> getParam(String longName) {
            return null;
        }

        public <T> Optional<T> getParamValue(ParamMarker<T> marker) {

        }
    }

    private final class ParamMarker<T> {
        T getValue();
    }

    private final static class Param {
        String longForm;
        String shortForm;
    }
}
