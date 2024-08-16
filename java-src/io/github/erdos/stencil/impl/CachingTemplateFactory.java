package io.github.erdos.stencil.impl;

import io.github.erdos.stencil.PrepareOptions;
import io.github.erdos.stencil.PreparedTemplate;
import io.github.erdos.stencil.TemplateFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Wraps a TemplateFactory instance and proxies calls only iff template file has not been changed since last call.
 */
@SuppressWarnings("unused")
public final class CachingTemplateFactory implements TemplateFactory {
    private final TemplateFactory templateFactory;
    private final Map<Path, PreparedTemplate> cache = new ConcurrentHashMap<>();

    /**
     * Constructs a new wrapping instance. Caches in memory.
     *
     * @param templateFactory instance to wrap
     * @throws IllegalArgumentException on null input.
     */
    public CachingTemplateFactory(TemplateFactory templateFactory) {
        if (templateFactory == null)
            throw new IllegalArgumentException("can not wrap null object!");

        this.templateFactory = templateFactory;
    }

    @Override
    public PreparedTemplate prepareTemplateFile(Path templateFile, PrepareOptions options) throws IOException {
        if (cache.containsKey(templateFile)) {
            PreparedTemplate stored = cache.get(templateFile);
            long fileLastModified = Files.getLastModifiedTime(templateFile).toMillis() / 1000;
            if (stored.creationDateTime().toEpochSecond(ZoneOffset.UTC) <= fileLastModified) {
                // TODO: this is so not thread safe.
                stored.close();
                stored = templateFactory.prepareTemplateFile(templateFile, options);
                cache.put(templateFile, stored);
            }
            return stored;
        } else {
            final PreparedTemplate stored = templateFactory.prepareTemplateFile(templateFile, PrepareOptions.options());
            cache.put(templateFile, stored);
            return stored;
        }
    }
}
