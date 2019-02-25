package io.github.erdos.stencil.standalone;

import io.github.erdos.stencil.EvaluatedDocument;
import io.github.erdos.stencil.PreparedTemplate;
import io.github.erdos.stencil.TemplateData;
import io.github.erdos.stencil.impl.FileHelper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static io.github.erdos.stencil.API.prepare;
import static io.github.erdos.stencil.API.render;
import static io.github.erdos.stencil.impl.FileHelper.extension;
import static io.github.erdos.stencil.impl.FileHelper.removeExtension;
import static io.github.erdos.stencil.standalone.Parser.maybeDataFileFormat;
import static io.github.erdos.stencil.standalone.StencilArgsParser.JOBS_FILE;

public class StandaloneApplication {

    private final ArgsParser.ParseResult parsed;
    private final File outputDir;
    private final boolean overwriteOutput;

    public StandaloneApplication(ArgsParser.ParseResult parsed) {
        this.parsed = parsed;
        this.outputDir = StencilArgsParser.getOutputDirectory(parsed);
        this.overwriteOutput = StencilArgsParser.getOutputOverwritten(parsed);
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
        
        final AtomicReference<Iterator<String>> rest = new AtomicReference<>(parsed.getRestArgs().iterator());

        parsed.getParamValue(JOBS_FILE).map(jobsFile -> {
            try {
                return Stream.concat(Files.lines(jobsFile.toPath()), parsed.getRestArgs().stream()).iterator();
            } catch (IOException e) {
                throw new RuntimeException("Error reading jobs file: " + e);
            }
        }).ifPresent(rest::set);

        while (rest.get().hasNext()) {
            final File templateFile = new File(rest.get().next()).getAbsoluteFile();
            final PreparedTemplate template = prepare(templateFile);

            while (rest.get().hasNext()) {
                final File dataFile = new File(rest.get().next()).getAbsoluteFile();
                final Optional<Parser.DataFileFormat> format = maybeDataFileFormat(dataFile);
                if (!format.isPresent()) {
                    throw new IllegalArgumentException("Unknown extension, only JSON and EDN are supported: " + dataFile);
                }

                final Object data = format.get().parse(dataFile);
                if (!(data instanceof Map)) {
                    throw new IllegalArgumentException("Template data is not a map in file: " + dataFile + " of " + data.getClass());
                } else {
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

    private static File targetFile(File targetDirectory, File template, File data) {
        final String part1 = template.getName();
        final String part2 = removeExtension(data);
        final String ext = extension(template);
        return new File(targetDirectory, part1 + "-" + part2 + "." + ext);
    }
}
