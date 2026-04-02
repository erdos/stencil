package io.github.erdos.stencil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public interface TemplateFactory {

    @Deprecated
    default PreparedTemplate prepareTemplateFile(File inputTemplateFile, PrepareOptions options) throws IOException {
        return prepareTemplateFile(inputTemplateFile.toPath(), options);
    }

    @Deprecated
    default PreparedTemplate prepareTemplateFile(File inputTemplateFile) throws IOException {
        return prepareTemplateFile(inputTemplateFile.toPath());
    }

    /**
     * Preprocesses a raw template file.
     *
     * @param inputTemplateFile raw template file of known type.
     * @param options template preparation options.
     * @return preprocessed template file
     * @throws IOException                   on file system error
     * @throws IllegalArgumentException      when argument is null, unknown type or does not exist
     * @throws java.io.FileNotFoundException when file does not exist
     */
    PreparedTemplate prepareTemplateFile(Path inputTemplateFile, PrepareOptions options) throws IOException;

    default PreparedTemplate prepareTemplateFile(Path inputTemplateFile) throws IOException {
        return prepareTemplateFile(inputTemplateFile, PrepareOptions.options());
    }
}
