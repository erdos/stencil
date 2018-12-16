package io.github.erdos.stencil;

import io.github.erdos.stencil.standalone.ArgsParser;
import io.github.erdos.stencil.standalone.StandaloneApplication;
import io.github.erdos.stencil.standalone.StencilArgsParser;

/**
 * Use this class for running Stencil in standalone mode.
 *
 * Parameters:
 *
 *
 * Usage: java io.github.erdos.stencil.Main [options ...] [--] SOURCEFILE.docx DATA1.json DATA2.json ... DATAn.json
 *
 * option keys:
 * -i inputfile, --input=inputfile - read input file names from a file (line-by-line)
 *
 */
public class Main {

    public static void main(String... args) {

        ArgsParser.ParseResult parsed = StencilArgsParser.parse(args);


        final StandaloneApplication application = new StandaloneApplication();

    }
}
