package io.github.erdos.stencil.functions;

import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

final class FunctionLoader {
    private FunctionLoader() {}

    private static final ServiceLoader<FunctionProvider> REGISTRARS = ServiceLoader.load(FunctionProvider.class);

    static List<Function> getFunctions() {
        return StreamSupport.stream(REGISTRARS.spliterator(), false)
                .sorted(Comparator.comparingInt(FunctionProvider::priority))
                .flatMap(p -> p.functions().stream())
                .collect(Collectors.toList());
    }
}
