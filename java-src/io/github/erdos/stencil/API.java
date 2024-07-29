package io.github.erdos.stencil;

import io.github.erdos.stencil.functions.Function;
import io.github.erdos.stencil.functions.FunctionEvaluator;
import io.github.erdos.stencil.impl.NativeTemplateFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

public final class API {

    private API() {}

    /**
     * Prepares a document template file from the file system.
     */
    public static PreparedTemplate prepare(File templateFile) throws IOException {
        return prepare(templateFile, PrepareOptions.options());
    }

    /**
     * Prepares a document template file from the file system.
     */
    public static PreparedTemplate prepare(File templateFile, PrepareOptions options) throws IOException {
        return new NativeTemplateFactory().prepareTemplateFile(templateFile, options);
    }

    /**
     * Prepares a document fragment from the file system. Fragments can be used to embed extra content when rendering
     * document templates. For example, custom headers and footers can be reused across documents this way.
     *
     * @param fragmentFile template file from file system to be used as document fragment
     * @return fragment instance, not null
     * @throws IllegalArgumentException      when fragmentFile is null
     * @throws IOException                   on file system error
     * @throws java.io.FileNotFoundException when file is not found on file system
     */
    public static PreparedFragment fragment(File fragmentFile, PrepareOptions options) throws IOException {
        return new NativeTemplateFactory().prepareFragmentFile(fragmentFile, options);
    }

    public static PreparedFragment fragment(File fragmentFile) throws IOException {
        return fragment(fragmentFile, PrepareOptions.options());
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
            function.registerFunctions(() -> new ArrayList<>(customFunctions));
        }
        return template.render(fragments, function, data);
    }
}
