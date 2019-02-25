package io.github.erdos.stencil.standalone;


import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Optional;

import static io.github.erdos.stencil.impl.FileHelper.extension;
import static io.github.erdos.stencil.standalone.Parser.DataFileFormat.EDN;
import static io.github.erdos.stencil.standalone.Parser.DataFileFormat.JSON;

public class Parser {

    public static Optional<DataFileFormat> maybeDataFileFormat(File file) {
        final String extension = extension(file);

        if ("json".equals(extension)) {
            return Optional.of(JSON);
        } else if ("edn".equals(extension)) {
            return Optional.of(EDN);
        } else {
            return Optional.empty();
        }
    }

    // TODO: inpect the opportunity of using streams.
    private static String fileContents(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    public enum DataFileFormat {
        JSON {
            @Override
            public Object parse(File input) throws IOException {
                return JsonParser.parse(fileContents(input));
            }
        },
        EDN {
            @Override
            public Object parse(File input) throws IOException {
                return EdnParser.parse(fileContents(input)).get();
            }
        };

        public abstract Object parse(File input) throws IOException;
    }
}
