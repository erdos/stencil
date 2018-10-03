package io.github.erdos.stencil;

import java.io.File;
import java.io.IOException;

public interface TemplateFactory {

    /**
     * Preprocesses a raw template file.
     *
     * @param templateFile raw template file of known type.
     * @return preprocessed template file
     * @throws IOException              on file system error
     * @throws IllegalArgumentException when argument is null, unknown type or does not exist
     * @throws java.io.FileNotFoundException when file does not exist
     */
    PreparedTemplate prepareTemplateFile(File templateFile) throws IOException;
}
