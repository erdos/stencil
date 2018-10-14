package io.github.erdos.stencil.standalone;

import java.util.List;
import java.util.Map;

import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;

public class ArgsParser {

    /**
     * Represents a vararg argument list
     */
    public final static Param REST = new Param();

    public void addParam(String shortForm, String longForm) {

    }

    public ParseResult parse(String... args) {

        return null;
    }

    public final class ParseResult {
        final Map<Param, String> args;
        final List<String> varargs;

        public ParseResult(Map<Param, String> args, List<String> varargs) {
            this.args = unmodifiableMap(args);
            this.varargs = unmodifiableList(varargs);
        }
    }

    private final static class Param {
        String longForm;
        String shortForm;
    }
}
