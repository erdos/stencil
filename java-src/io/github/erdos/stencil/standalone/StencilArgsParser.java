package io.github.erdos.stencil.standalone;

public final class StencilArgsParser {

    private final static ArgsParser PARSER = new ArgsParser();

    static {
        PARSER.addFlagOption('s', "schema", "Print template schema", false);


        PARSER.addParam('o', "output", "Output directory path");

        PARSER.addParam('i', "stdin", "Input file paths read from standard input");


        PARSER.addParam('f', "format", "Data format, either 'json' or 'edn'");

    }

    public static ArgsParser.ParseResult parse(String... args) {
        return PARSER.parse(args);
    }
}
