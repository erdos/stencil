package io.github.erdos.stencil;

import io.github.erdos.stencil.standalone.ArgsParser;
import io.github.erdos.stencil.standalone.StandaloneApplication;

/**
 * Use this class for running Stencil in standalone mode.
 *
 * Parameters:
 *
 *
 * Usage: java io.github.erdos.stencil.Main [options ...] SOURCEFILE.docx -- DATA1.json DATA2.json ... DATAn.json
 *
 * option keys:
 * -i inputfile, --input=inputfile - read input file names from a file (line-by-line)
 *
 */
public class Main {

    public static void main(String... args) {

        ArgsParser parser = new ArgsParser(args);

        StandaloneApplication application = new StandaloneApplication();



    }
}
