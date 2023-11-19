package io.github.erdos.stencil.impl;

import io.github.erdos.stencil.EvaluatedDocument;
import io.github.erdos.stencil.PreparedFragment;
import io.github.erdos.stencil.PreparedTemplate;
import io.github.erdos.stencil.TemplateData;
import io.github.erdos.stencil.functions.FunctionEvaluator;

import java.util.Map;

/**
 * Default implementation that calls the engine written in Clojure.
 */
public final class NativeEvaluator {

    private final FunctionEvaluator functions = new FunctionEvaluator();


    /**
     * Evaluates a preprocessed template using the given data.
     *
     * @param template preprocessed template file
     * @param data     contains template variables
     * @return evaluated document ready to save to fs
     * @throws IllegalArgumentException when any arg is null
     */
    public EvaluatedDocument render(PreparedTemplate template, Map<String, PreparedFragment> fragments, TemplateData data) {
        if (template == null) {
            throw new IllegalArgumentException("Template object is missing!");
        } else if (data == null) {
            throw new IllegalArgumentException("Template data is missing!");
        }
        return template.render(fragments, functions, data);
    }


    /**
     * It can be used to externally add function definitions.
     */
    @SuppressWarnings("unused")
    public FunctionEvaluator getFunctionEvaluator() {
        return functions;
    }
}