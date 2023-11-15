package io.github.erdos.stencil.impl;

import clojure.lang.AFunction;
import clojure.lang.IFn;
import clojure.lang.PersistentHashMap;
import io.github.erdos.stencil.EvaluatedDocument;
import io.github.erdos.stencil.PreparedFragment;
import io.github.erdos.stencil.PreparedTemplate;
import io.github.erdos.stencil.TemplateData;
import io.github.erdos.stencil.exceptions.EvalException;
import io.github.erdos.stencil.functions.FunctionEvaluator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

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

        final IFn fn = ClojureHelper.findFunction("eval-template");
        final Object argsMap = makeArgsMap(template.getSecretObject(), fragments, data.getData());

        try {
            return (EvaluatedDocument) fn.invoke(argsMap);
        } catch (EvalException e) {
            throw e;
        } catch (Exception e) {
            throw new EvalException("Unexpected error", e);
        }
    }


    /**
     * It can be used to externally add function definitions.
     */
    @SuppressWarnings("unused")
    public FunctionEvaluator getFunctionEvaluator() {
        return functions;
    }

    @SuppressWarnings("unchecked")
    private Object makeArgsMap(Object template, Map<String, PreparedFragment> fragments, Object data) {
        final Map result = new HashMap();
        result.put(ClojureHelper.Keywords.TEMPLATE.kw, template);
        result.put(ClojureHelper.Keywords.DATA.kw, data);
        result.put(ClojureHelper.Keywords.FUNCTION.kw, new FunctionCaller());

        // string to clojure map
        final Map<String, Object> kvs = fragments.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, v -> v.getValue().getImpl()));
        result.put(ClojureHelper.Keywords.FRAGMENTS.kw, PersistentHashMap.create(kvs));

        return PersistentHashMap.create(result);
    }

    private final class FunctionCaller extends AFunction {

        /**
         * @param functionName callable fn name
         * @param argsList     a Collection of arguments
         */
        @Override
        @SuppressWarnings("unchecked")
        public Object invoke(Object functionName, Object argsList) {
            if (!(functionName instanceof String)) {
                throw new IllegalArgumentException("First argument must be a String!");
            } else if (argsList == null) {
                argsList = emptyList();
            } else if (!(argsList instanceof Collection)) {
                throw new IllegalArgumentException("Second argument must be a collection!");
            }

            final Object[] args = new ArrayList((Collection) argsList).toArray();

            return functions.call(functionName.toString(), args);
        }
    }
}