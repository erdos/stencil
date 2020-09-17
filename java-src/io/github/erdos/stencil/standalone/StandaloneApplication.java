package io.github.erdos.stencil.standalone;

import clojure.lang.IFn;
import clojure.lang.RT;
import clojure.lang.Symbol;
import io.github.erdos.stencil.EvaluatedDocument;
import io.github.erdos.stencil.PrepareOptions;
import io.github.erdos.stencil.PreparedTemplate;
import io.github.erdos.stencil.TemplateData;
import io.github.erdos.stencil.impl.ClojureHelper;
import io.github.erdos.stencil.impl.FileHelper;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Stream;

import static io.github.erdos.stencil.API.prepare;
import static io.github.erdos.stencil.API.render;
import static io.github.erdos.stencil.impl.FileHelper.extension;
import static io.github.erdos.stencil.impl.FileHelper.removeExtension;
import static io.github.erdos.stencil.standalone.Parser.maybeDataFileFormat;
import static io.github.erdos.stencil.standalone.StencilArgsParser.JOBS_FILE;
import static io.github.erdos.stencil.standalone.StencilArgsParser.JOBS_FROM_STDIN;

public class StandaloneApplication {

    private final ArgsParser.ParseResult parsed;
    private final File outputDir;
    private final boolean overwriteOutput;
    private final PrepareOptions prepareOptions;

    public StandaloneApplication(ArgsParser.ParseResult parsed) {
        this.parsed = parsed;
        this.outputDir = StencilArgsParser.getOutputDirectory(parsed);
        this.overwriteOutput = StencilArgsParser.getOutputOverwritten(parsed);

        if (StencilArgsParser.getOnlyIncludes(parsed)) {
            this.prepareOptions = PrepareOptions.options().withOnlyIncludes();
        } else {
            this.prepareOptions = PrepareOptions.options();
        }
    }

    private void checkRestFilesExist() {
        for (String file : parsed.getRestArgs()) {
            if (!new File(file).exists()) {
                throw new IllegalArgumentException("File does not exist: " + file);
            }
            // TODO: also check that this is a readable data file.
        }
    }

    public void run() throws IOException {

        checkRestFilesExist();

        final Iterator<String> rest = jobsIterator();

        if (parsed.getParamValue(StencilArgsParser.SHOW_VERSION).orElse(false)) {
            displayVersionInfo();
        } else if (!rest.hasNext() || parsed.getParamValue(StencilArgsParser.SHOW_HELP).orElse(false)) {
            displayHelpInfo();
        } else {
            try {
                processJobs(rest);
            } catch (EndOfFilesException ignored) {
                // processed all files
            }
        }
    }

    private void processJobs(Iterator<String> rest) throws IOException {
        while (rest.hasNext()) {
            final File templateFile = new File(rest.next()).getAbsoluteFile();
            final PreparedTemplate template = prepare(templateFile, prepareOptions);

            while (rest.hasNext()) {
                final File dataFile = new File(rest.next()).getAbsoluteFile();
                final Optional<Parser.DataFileFormat> format = maybeDataFileFormat(dataFile);
                if (!format.isPresent()) {
                    throw new IllegalArgumentException("Unknown extension, only JSON and EDN are supported: " + dataFile);
                }

                final Object data = format.get().parse(dataFile);
                if (!(data instanceof Map)) {
                    throw new IllegalArgumentException("Template data is not a map in file: " + dataFile + " of " + data.getClass());
                } else {
                    //noinspection unchecked
                    final TemplateData templateData = TemplateData.fromMap((Map) data);
                    final EvaluatedDocument document = render(template, templateData);

                    final File targetFile = targetFile(outputDir, templateFile, dataFile);
                    if (targetFile.exists() && overwriteOutput) {
                        FileHelper.forceDelete(targetFile);
                    }
                    document.writeToFile(targetFile);
                }
            }
        }
    }

    private Iterator<String> jobsIterator() {
        final Optional<File> jobsFile = parsed.getParamValue(JOBS_FILE);
        final Optional<Boolean> jobsStdin = parsed.getParamValue(JOBS_FROM_STDIN);

        if (jobsStdin.isPresent() && jobsFile.isPresent()) {
            throw new IllegalArgumentException("Can not specify both the --stdin and --jobs parameters!");
        } else if (jobsFile.isPresent()) {
            if (!parsed.getRestArgs().isEmpty()) {
                throw new IllegalArgumentException("Can not specify both --jobs and template parameters!");
            } else {
                try {
                    return Files.lines(jobsFile.get().toPath()).iterator();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        } else if (jobsStdin.isPresent()) {
            if (!parsed.getRestArgs().isEmpty()) {
                throw new IllegalArgumentException("Can not specify both --stdin and template parameters!");
            } else {
                return Stream.generate(() -> {
                    try {
                        return new Scanner(System.in).nextLine();
                    } catch (NoSuchElementException e) {
                        throw new EndOfFilesException();
                    }
                }).iterator();
            }
        } else {
            return parsed.getRestArgs().iterator();
        }
    }

    private static final class EndOfFilesException extends RuntimeException {
    }

    private static File targetFile(File targetDirectory, File template, File data) {
        final String part1 = removeExtension(template);
        final String part2 = removeExtension(data);
        final String ext = extension(template);
        return new File(targetDirectory, part1 + "-" + part2 + "." + ext);
    }

    private void displayHelpInfo() throws IOException {
        final URL url = getClass().getResource("help.txt");
        if (url == null) {
            throw new IllegalStateException("Missing help.txt file!");
        }

        try (final BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()))) {
            for (String inputLine; (inputLine = in.readLine()) != null; ) {
                System.out.println(inputLine);
            }
        }
    }

    private void displayVersionInfo() {
        RT.var("clojure.core", "require").invoke(Symbol.intern("stencil.api"));
        System.out.println(RT.var("stencil.api", "version").deref().toString());
    }
}
