package io.github.erdos.stencil;

import io.github.erdos.stencil.functions.FunctionEvaluator;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Represents an already preprocessed template file.
 * <p>
 * These files may be serialized or cached for later use.
 */
@SuppressWarnings("unused")
public interface PreparedTemplate extends AutoCloseable {

    /**
     * Original template file that was preprocessed.
     *
     * @return original template file
     */
    Path getTemplateFile();

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
    LocalDateTime creationDateTime(); // TODO: remove?


    /**
     * Set of template variables found in file.
     */
    TemplateVariables getVariables();

    /**
     * Makes the template clean up any resources allocated for it.
     * Subsequent invocations of this method have no effects.
     * Rendering the template after this method call will throw an IllegalStateException.
     */
    @Override
    void close();

    /**
     * Renders the current prepared template file with the given template data.
     */
    EvaluatedDocument render(Map<String, PreparedFragment> fragments, FunctionEvaluator function, TemplateData templateData);
}