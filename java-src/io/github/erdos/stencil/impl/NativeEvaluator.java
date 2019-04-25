package io.github.erdos.stencil.impl;

import clojure.lang.AFunction;
import clojure.lang.IFn;
import io.github.erdos.stencil.*;
import io.github.erdos.stencil.exceptions.EvalException;
import io.github.erdos.stencil.functions.FunctionEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static io.github.erdos.stencil.impl.Logging.debugStopWatch;
import static java.util.Collections.emptyList;

/**
 * Default implementation that calls the engine written in Clojure.
 */
public final class NativeEvaluator {

    private final static Logger LOGGER = LoggerFactory.getLogger(NativeEvaluator.class);

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

        final Consumer<Supplier<String>> stopwatch = debugStopWatch(LOGGER);
        stopwatch.accept(() -> "Starting document rendering for template " + template.getTemplateFile());

        final IFn fn = ClojureHelper.findFunction("eval-template");
        final Map argsMap = makeArgsMap(template.getSecretObject(), fragments, data.getData());

        final Map result;
        try {
            result = (Map) fn.invoke(argsMap);
        } catch (Exception e) {
            throw EvalException.wrapping(e);
        }

        final Consumer<OutputStream> stream = resultWriter(result);

        return build(stream, template.getTemplateFormat());
    }


    /**
     * It can be used to externally add function definitions.
     */
    @SuppressWarnings("unused")
    public FunctionEvaluator getFunctionEvaluator() {
        return functions;
    }

    private static EvaluatedDocument build(Consumer<OutputStream> writer, TemplateDocumentFormats format) {
        return new EvaluatedDocument() {

            @Override
            public TemplateDocumentFormats getFormat() {
                return format;
            }

            @Override
            public Consumer<OutputStream> getWriter() {
                return writer;
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static Consumer<OutputStream> resultWriter(Map result) {
        IFn writer = (IFn) ClojureHelper.Keywords.WRITER.getOrThrow(result);
        return writer::invoke;
    }

    @SuppressWarnings("unchecked")
    private Map makeArgsMap(Object template, Map<String, PreparedFragment> fragments, Object data) {
        final Map result = new HashMap();
        result.put(ClojureHelper.Keywords.TEMPLATE.kw, template);
        result.put(ClojureHelper.Keywords.DATA.kw, data);
        result.put(ClojureHelper.Keywords.FUNCTION.kw, new FunctionCaller());

        // string to clojure map
        final Map<String, Object> kvs = fragments.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, v -> v.getValue().getImpl()));
        result.put(ClojureHelper.Keywords.FRAGMENTS.kw, kvs);

        return result;
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