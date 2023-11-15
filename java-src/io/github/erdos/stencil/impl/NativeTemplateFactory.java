package io.github.erdos.stencil.impl;

import clojure.lang.IFn;
import clojure.lang.Keyword;
import io.github.erdos.stencil.*;
import io.github.erdos.stencil.exceptions.ParsingException;

import java.io.File;
import java.io.IOException;
import java.util.*;

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

        try {
            return (PreparedTemplate) ClojureHelper.findFunction("prepare-template").invoke(inputTemplateFile, options);
        } catch (ParsingException e) {
            throw e;
        } catch (Exception e) {
            throw ParsingException.wrapping("Could not parse template file!", e);
        }
    }

    public PreparedFragment prepareFragmentFile(final File fragmentFile, PrepareOptions options) throws IOException {
        if (fragmentFile == null) {
            throw new IllegalArgumentException("Fragment file parameter is null!");
        }

        final IFn prepareFunction = ClojureHelper.findFunction("prepare-fragment");

        final Map<Keyword, Object> prepared;

        try {
            prepared = invokePrepareFunction(fragmentFile, options);
        } catch (ParsingException | IOException e) {
            throw e;
        } catch (Exception e) {
            throw ParsingException.wrapping("Could not parse template file!", e);
        }

        final File zipDirResource = (File) prepared.get(ClojureHelper.Keywords.SOURCE_FOLDER.kw);
        if (zipDirResource != null) {
            forceDeleteOnExit(zipDirResource);
        }

        return new PreparedFragment(prepared, zipDirResource);
    }

    @SuppressWarnings({"unchecked", "RedundantThrows"})
    private static Map<Keyword, Object> invokePrepareFunction(File fragmentFile, PrepareOptions options) throws Exception {
        final IFn prepareFunction = ClojureHelper.findFunction("prepare-fragment");
        return (Map<Keyword, Object>) prepareFunction.invoke(fragmentFile, options);
    }

    /**
     * Retrieves content of :variables keyword from map as a set.
     */
    @SuppressWarnings("unchecked")
    private static Set<String> variableNames(Map prepared) {
        return prepared.containsKey(ClojureHelper.Keywords.VARIABLES.kw)
                ? unmodifiableSet(new HashSet<>((Collection<String>) prepared.get(ClojureHelper.Keywords.VARIABLES.kw)))
                : emptySet();
    }

    @SuppressWarnings("unchecked")
    private static Set<String> fragmentNames(Map prepared) {
        return prepared.containsKey(ClojureHelper.Keywords.FRAGMENTS.kw)
                ? unmodifiableSet(new HashSet<>((Collection<String>) prepared.get(ClojureHelper.Keywords.FRAGMENTS.kw)))
                : emptySet();
    }
}
