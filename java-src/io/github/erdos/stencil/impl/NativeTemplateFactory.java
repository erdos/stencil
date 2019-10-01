package io.github.erdos.stencil.impl;

import clojure.lang.IFn;
import clojure.lang.Keyword;
import io.github.erdos.stencil.*;
import io.github.erdos.stencil.exceptions.ParsingException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.github.erdos.stencil.TemplateDocumentFormats.ofExtension;
import static io.github.erdos.stencil.impl.FileHelper.forceDeleteOnExit;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableSet;

@SuppressWarnings("unused")
public final class NativeTemplateFactory implements TemplateFactory {

    @Override
    public PreparedTemplate prepareTemplateFile(final File inputTemplateFile, PrepareOptions options) throws IOException {
        final Optional<TemplateDocumentFormats> templateDocFormat = ofExtension(inputTemplateFile.getName());

        if (!templateDocFormat.isPresent()) {
            throw new IllegalArgumentException("Unexpected type of file: " + inputTemplateFile.getName());
        }

        if (options == null) {
            throw new IllegalArgumentException("Template preparation options are missing!");
        }

        try (InputStream input = new FileInputStream(inputTemplateFile)) {
            return prepareTemplateImpl(templateDocFormat.get(), input, inputTemplateFile, options);
        }
    }

    public PreparedFragment prepareFragmentFile(final File fragmentFile, PrepareOptions options) throws IOException {
        if (fragmentFile == null) {
            throw new IllegalArgumentException("Fragment file parameter is null!");
        }

        final IFn prepareFunction = ClojureHelper.findFunction("prepare-fragment");

        final Map<Keyword, Object> prepared;

        try {
            //noinspection unchecked
            prepared = (Map<Keyword, Object>) prepareFunction.invoke(fragmentFile, options);
        } catch (ParsingException e) {
            throw e;
        } catch (Exception e) {
            //noinspection ConstantConditions
            if (e instanceof IOException) {
                // possible because of Clojure magic :-(
                throw (IOException) e;
            } else {
                throw ParsingException.wrapping("Could not parse template file!", e);
            }
        }

        final File zipDirResource = (File) prepared.get(ClojureHelper.Keywords.SOURCE_FOLDER.kw);
        if (zipDirResource != null) {
            forceDeleteOnExit(zipDirResource);
        }

        return new PreparedFragment(prepared, zipDirResource);
    }

    /**
     * Retrieves content of :variables keyword from map as a set.
     */
    @SuppressWarnings("unchecked")
    private Set variableNames(Map prepared) {
        return prepared.containsKey(ClojureHelper.Keywords.VARIABLES.kw)
                ? unmodifiableSet(new HashSet<Set>((Collection) prepared.get(ClojureHelper.Keywords.VARIABLES.kw)))
                : emptySet();
    }

    @SuppressWarnings("unchecked")
    private Set fragmentNames(Map prepared) {
        return prepared.containsKey(ClojureHelper.Keywords.FRAGMENTS.kw)
                ? unmodifiableSet(new HashSet<Set>((Collection) prepared.get(ClojureHelper.Keywords.FRAGMENTS.kw)))
                : emptySet();
    }

    @SuppressWarnings("unchecked")
    private PreparedTemplate prepareTemplateImpl(TemplateDocumentFormats templateDocFormat, InputStream input, File originalFile, PrepareOptions options) {
        final IFn prepareFunction = ClojureHelper.findFunction("prepare-template");

        final String format = templateDocFormat.name();
        final Map<Keyword, Object> prepared;

        try {
            prepared = (Map<Keyword, Object>) prepareFunction.invoke(input, options);
        } catch (ParsingException e) {
            throw e;
        } catch (Exception e) {
            throw ParsingException.wrapping("Could not parse template file!", e);
        }

        final TemplateVariables vars = TemplateVariables.fromPaths(variableNames(prepared), fragmentNames(prepared));

        final File zipDirResource = (File) prepared.get(ClojureHelper.Keywords.SOURCE_FOLDER.kw);
        if (zipDirResource != null) {
            forceDeleteOnExit(zipDirResource);
        }

        return new PreparedTemplate() {
            final LocalDateTime now = LocalDateTime.now();
            final AtomicBoolean valid = new AtomicBoolean(true);

            @Override
            public File getTemplateFile() {
                return originalFile;
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
                if (!valid.get()) {
                    throw new IllegalStateException("Can not render destroyed template!");
                } else {
                    return prepared;
                }
            }

            @Override
            public TemplateVariables getVariables() {
                return vars;
            }

            @Override
            public void cleanup() {
                if (valid.compareAndSet(true, false)) {
                    // deletes unused temporary zip directory
                    if (zipDirResource != null) {
                        FileHelper.forceDelete(zipDirResource);
                    }
                }
            }

            @Override
            public String toString() {
                return "<Template from file " + originalFile + ">";
            }
        };
    }
}
