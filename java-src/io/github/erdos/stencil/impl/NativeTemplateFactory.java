package io.github.erdos.stencil.impl;

import io.github.erdos.stencil.*;
import io.github.erdos.stencil.exceptions.ParsingException;

import java.nio.file.Path;
import java.util.Optional;

import static io.github.erdos.stencil.TemplateDocumentFormats.ofExtension;

@SuppressWarnings("unused")
public final class NativeTemplateFactory implements TemplateFactory {

    @Override
    public PreparedTemplate prepareTemplateFile(final Path inputTemplateFile, PrepareOptions options) {
        final Optional<TemplateDocumentFormats> templateDocFormat = ofExtension(inputTemplateFile.toString());

        if (!templateDocFormat.isPresent()) {
            throw new IllegalArgumentException("Unexpected type of file: " + inputTemplateFile);
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

    public PreparedFragment prepareFragmentFile(final Path fragmentSource, PrepareOptions options) {
        if (fragmentSource == null) {
            throw new IllegalArgumentException("Fragment source parameter is null!");
        }

        if (options == null) {
            throw new IllegalArgumentException("Template preparation options are missing!");
        }

        try {
            return (PreparedFragment) ClojureHelper.findFunction("prepare-fragment").invoke(fragmentSource, options);
        } catch (ParsingException e) {
            throw e;
        } catch (Exception e) {
            throw ParsingException.wrapping("Could not parse fragment template file!", e);
        }
    }
}
