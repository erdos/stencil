package io.github.erdos.stencil.impl;

import io.github.erdos.stencil.PrepareOptions;
import io.github.erdos.stencil.PreparedTemplate;
import io.github.erdos.stencil.TemplateFactory;

import java.io.File;
import java.io.IOException;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Wraps a TemplateFactory instance and proxies calls only iff template file has not been changed since last call.
 */
@SuppressWarnings("unused")
public final class CachingTemplateFactory implements TemplateFactory {
    private final TemplateFactory templateFactory;
    private final Map<File, PreparedTemplate> cache = new ConcurrentHashMap<>();

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
    public PreparedTemplate prepareTemplateFile(File templateFile, PrepareOptions options) throws IOException {
        if (cache.containsKey(templateFile)) {
            PreparedTemplate stored = cache.get(templateFile);
            if (stored.creationDateTime().toEpochSecond(ZoneOffset.UTC) <= templateFile.lastModified()) {
                // TODO: this is so not thread safe.
                stored.cleanup();
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
