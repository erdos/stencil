package io.github.erdos.stencil.impl;

import clojure.lang.IFn;
import clojure.lang.Keyword;
import io.github.erdos.stencil.PreparedTemplate;
import io.github.erdos.stencil.TemplateDocumentFormats;
import io.github.erdos.stencil.TemplateFactory;
import io.github.erdos.stencil.TemplateVariables;
import io.github.erdos.stencil.exceptions.ParsingException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;

import static io.github.erdos.stencil.TemplateDocumentFormats.ofExtension;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableSet;

@SuppressWarnings("unused")
public final class NativeTemplateFactory implements TemplateFactory {

    @Override
    public PreparedTemplate prepareTemplateFile(final File inputTemplateFile) throws IOException {
        final Optional<TemplateDocumentFormats> templateDocFormat = ofExtension(inputTemplateFile.getName());

        if (!templateDocFormat.isPresent()) {
            throw new IllegalArgumentException("Unexpected type of file: " + inputTemplateFile.getName());
        }

        try (InputStream input = new FileInputStream(inputTemplateFile)) {
            return prepareTemplateImpl(templateDocFormat.get(), input);
        }
    }

    /**
     * Retrieves content of :variables keyword from map as a set.
     */
    @SuppressWarnings("unchecked")
    private Set variableNames(Map prepared) {
        return prepared.containsKey(ClojureHelper.KV_VARIABLES)
                ? unmodifiableSet(new HashSet<Set>((Collection) prepared.get(ClojureHelper.KV_VARIABLES)))
                : emptySet();
    }

    @SuppressWarnings("unchecked")
    private PreparedTemplate prepareTemplateImpl(TemplateDocumentFormats templateDocFormat, InputStream input) {
        final IFn prepareFunction = ClojureHelper.findFunction("prepare-template");

        final String format = templateDocFormat.name();
        final Map<Keyword, Object> prepared;

        try {
            prepared = (Map<Keyword, Object>) prepareFunction.invoke(format, input);
        } catch (ParsingException e) {
            throw e;
        } catch (Exception e) {
            throw ParsingException.wrapping("Could not parse template file!", e);
        }

        final String templateName = (String) prepared.get(Keyword.intern("template-name"));
        final File templateFile = (File) prepared.get(Keyword.intern("template-file"));
        final LocalDateTime now = LocalDateTime.now();
        final TemplateVariables vars = TemplateVariables.fromPaths(variableNames(prepared));

        return new PreparedTemplate() {
            @Override
            public String getName() {
                return templateName;
            }

            @Override
            public File getTemplateFile() {
                return templateFile;
            }

            @Override
            public TemplateDocumentFormats getTemplateFormat() {
                return templateDocFormat;
            }

            @Override
            public LocalDateTime creationDateTime() {
                return now;
            }

            @Override
            public Object getSecretObject() {
                return prepared;
            }

            @Override
            public TemplateVariables getVariables() {
                return vars;
            }
        };
    }
}