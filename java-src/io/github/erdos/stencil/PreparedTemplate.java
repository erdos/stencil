package io.github.erdos.stencil;

import java.io.File;
import java.time.LocalDateTime;

/**
 * Represents an already preprocessed template file.
 * <p>
 * These files may be serialized or cached for later use.
 */
@SuppressWarnings("unused")
public interface PreparedTemplate extends AutoCloseable{

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

    /**
     * Makes the template clean up any resources allocated for it. Subsequential invocations of this method have no
     * effects. Rendering the template after this method call will throw an IllegalStateException.
     */
    void cleanup();

    /**
     * Renders the current prepared template file with the given template data.
     */
    default EvaluatedDocument render(TemplateData templateData) {
        return API.render(this, templateData);
    }

    @Override
    default void close() {
        cleanup();
    }
}