package io.github.erdos.stencil;

import io.github.erdos.stencil.impl.NativeEvaluator;
import io.github.erdos.stencil.impl.NativeTemplateFactory;

import java.io.File;
import java.io.IOException;

public final class API {

    public static PreparedTemplate prepare(File templateFile) throws IOException {
        return new NativeTemplateFactory().prepareTemplateFile(templateFile);
    }

    public static EvaluatedDocument render(PreparedTemplate template, TemplateData data) {
        return new NativeEvaluator().render(template, data);
    }
}
