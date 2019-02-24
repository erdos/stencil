package io.github.erdos.stencil;

import io.github.erdos.stencil.standalone.ArgsParser;
import io.github.erdos.stencil.standalone.Parser;
import io.github.erdos.stencil.standalone.StencilArgsParser;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import static io.github.erdos.stencil.API.prepare;
import static io.github.erdos.stencil.API.render;
import static io.github.erdos.stencil.impl.FileHelper.removeExtension;
import static io.github.erdos.stencil.standalone.Parser.maybeDataFileFormat;

public class Main {

    public static void main(String... args) throws IOException {

        ArgsParser.ParseResult parsed = StencilArgsParser.parse(args);

        final File outputDir = StencilArgsParser.getOutputDirectory(parsed);

        final Iterator<String> rest = parsed.getRestArgs().iterator();

        for (String file : parsed.getRestArgs()) {
            if (!new File(file).exists()) {
                throw new IllegalArgumentException("File does not exist: " + file);
            }
            // TODO: also check that this is a readable data file.
        }

        while (rest.hasNext()) {
            final File templateFile = new File(rest.next()).getAbsoluteFile();
            final PreparedTemplate template = prepare(templateFile);

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
                    final TemplateData templateData = TemplateData.fromMap((Map) data);
                    final EvaluatedDocument document = render(template, templateData);

                    final File targetFile = targetFile(outputDir, templateFile, dataFile);
                    document.writeToFile(targetFile);
                }

            }
        }
    }

    private static File targetFile(File targetDirectory, File template, File data) {
        return new File(targetDirectory, template.getName() + "-" + removeExtension(data) + ".docx");
    }
}
