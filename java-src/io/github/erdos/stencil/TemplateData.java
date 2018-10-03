package io.github.erdos.stencil;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Contains data to fill template documents. Immutable.
 */
public final class TemplateData {

    private final Map<String, Object> data;

    private TemplateData(Map<String, Object> data) {
        this.data = Collections.unmodifiableMap(Objects.requireNonNull(data));
    }

    /**
     * Construct a new empty template data object.
     */
    public static TemplateData empty() {
        return new TemplateData(Collections.emptyMap());
    }

    /**
     * Constructs a template data instance holding a map data structure.
     *
     * @param data map of template data. Possibly nested: values might contain maps or vectors recursively.
     * @return constructed data holder. Never null.
     * @throws IllegalArgumentException when input is null
     */
    @SuppressWarnings("unused")
    public static TemplateData fromMap(Map<String, Object> data) {
        if (data == null) {
            throw new IllegalArgumentException("Template data must not be null!");
        } else {
            return new TemplateData(data);
        }
    }

    /**
     * Returns contained data as a possibly nested map.
     *
     * @return template data map. Not null.
     */
    public final Map<String, Object> getData() {
        return data;
    }
}
