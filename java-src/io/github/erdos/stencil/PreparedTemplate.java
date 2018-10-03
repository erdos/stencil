package io.github.erdos.stencil;

import java.io.File;
import java.time.LocalDateTime;

/**
 * Represents an already preprocessed template file.
 * <p>
 * These files may be serialized or cached for later use.
 */
@SuppressWarnings("unused")
public interface
PreparedTemplate {

    /**
     * Name of the original file.
     *
     * @return original template name
     */
    String getName();

    /**
     * Original template file that was preprocessed.
     *
     * @return original template file
     */
    File getTemplateFile();

    /**
     * Format of template file. Tries to guess from file name by default.
     */
    default TemplateDocumentFormats getTemplateFormat() {
        return TemplateDocumentFormats
                .ofExtension(getTemplateFile().toString())
                .orElseThrow(() -> new IllegalStateException("Could not guess extension from file name " + getTemplateFile()));
    }

    /**
     * Time when the template was processed.
     *
     * @return template preprocess call time
     */
    LocalDateTime creationDateTime();

    /**
     * Contains the preprocess result.
     * <p>
     * Implementation detail. May be used for serializing these objects. May be used for debugging too.
     *
     * @return inner representation of prepared template
     */
    Object getSecretObject();


    /**
     * Set of template variables found in file.
     */
    TemplateVariables getVariables();
}