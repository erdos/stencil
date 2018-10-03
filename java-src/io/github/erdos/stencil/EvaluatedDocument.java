package io.github.erdos.stencil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

/**
 * An evaluated document ready to be converted to the final output format.
 */
public interface EvaluatedDocument {

    /**
     * Content of document as input stream.
     */
    InputStream getInputStream();

    TemplateDocumentFormats getFormat();

    /**
     * Writes output of this document to a file
     */
    default void writeToFile(File output) throws IOException {
        Files.copy(getInputStream(), output.toPath());
    }
}
