package io.github.erdos.stencil;

import io.github.erdos.stencil.functions.Function;
import io.github.erdos.stencil.functions.FunctionEvaluator;
import io.github.erdos.stencil.impl.NativeTemplateFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

public final class API {

    private API() {}

    /**
     * Prepares a document template file from the file system.
     * @deprecated use prepare(Path templateSource) instead.
     */
    public static PreparedTemplate prepare(File templateFile) throws IOException {
        return prepare(templateFile.toPath(), PrepareOptions.options());
    }

    /**
     * Prepares a document template file from the file system.
     * @deprecated use prepare(Path, PerpareOptions) instead.
     */
    public static PreparedTemplate prepare(File templateFile, PrepareOptions options) throws IOException {
        return prepare(templateFile.toPath(), options);
    }

    /**
     * Prepares a document template file from the file system.
     */
    public static PreparedTemplate prepare(Path templateSource) throws IOException {
        return prepare(templateSource, PrepareOptions.options());
    }

    /**
     * Prepares a document template file from the file system.
     */
    public static PreparedTemplate prepare(Path templateSource, PrepareOptions options) throws IOException {
        return new NativeTemplateFactory().prepareTemplateFile(templateSource, options);
    }

    /**
     * Prepares a document fragment from the file system. Fragments can be used to embed extra content when rendering
     * document templates. For example, custom headers and footers can be reused across documents this way.
     *
     * @param fragmentSource template file from file system to be used as document fragment
     * @return fragment instance, not null
     * @throws IllegalArgumentException      when fragmentFile is null
     * @throws IOException                   on file system error
     * @throws java.io.FileNotFoundException when file is not found on file system
     */
    public static PreparedFragment fragment(Path fragmentSource, PrepareOptions options) throws IOException {
        return new NativeTemplateFactory().prepareFragmentFile(fragmentSource, options);
    }

    public static PreparedFragment fragment(Path fragmentSource) throws IOException {
        return fragment(fragmentSource, PrepareOptions.options());
    }

    public static EvaluatedDocument render(PreparedTemplate template, TemplateData data) {
        return render(template, emptyMap(), data, emptyList());
    }

    public static EvaluatedDocument render(PreparedTemplate template, Map<String, PreparedFragment> fragments, TemplateData data) {
        return render(template, fragments, data, emptyList());
    }

    public static EvaluatedDocument render(PreparedTemplate template, Map<String, PreparedFragment> fragments, TemplateData data, Collection<Function> customFunctions) {
        FunctionEvaluator function = new FunctionEvaluator();
        if (customFunctions != null) {
            function.registerFunctions(customFunctions.toArray(new Function[0]));
        }
        return template.render(fragments, function, data);
    }
}
