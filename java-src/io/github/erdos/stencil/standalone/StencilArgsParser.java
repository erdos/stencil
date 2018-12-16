package io.github.erdos.stencil.standalone;

import java.io.File;

public final class StencilArgsParser {

    public final static ArgsParser PARSER = new ArgsParser();

    /**
     * Result files are placed in this directory.
     */
    public static final ArgsParser.ParamMarker<File> OUTPUT_DIR = PARSER.addParam('o', "output", "Output directory path", x -> {
        final File output = new File(x);
        if (!output.exists()) {
            throw new IllegalArgumentException("Output directory does not exist: " + output);
        } else if (!output.isDirectory()) {
            throw new IllegalArgumentException("Output directory parameter is not a directory: " + output);
        } else {
            return output;
        }
    });

    public static ArgsParser.ParseResult parse(String... args) {
        return PARSER.parse(args);
    }
}
