package io.xpipe.extension;

import java.util.HashSet;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;

public class DataSourceGuiProviders {

    private static Set<DataSourceGuiProvider> ALL;

    public static void loadAll(ModuleLayer layer) {
        if (ALL == null) {
            ALL = new HashSet<>();
            ALL.addAll(ServiceLoader.load(layer, DataSourceGuiProvider.class).stream()
                    .map(ServiceLoader.Provider::get).collect(Collectors.toSet()));
        }
    }

    public static Optional<DataSourceGuiProvider> byClass(Class<?> clazz) {
        if (ALL == null) {
            throw new IllegalStateException("Not initialized");
        }
        return ALL.stream().filter(d -> d.getType().equals(clazz)).findAny();
    }

    public static Set<DataSourceGuiProvider> getAll() {
        if (ALL == null) {
            throw new IllegalStateException("Not initialized");
        }
        return ALL;
    }
}
