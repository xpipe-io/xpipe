package io.xpipe.extension;

import java.nio.file.Path;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;

public class DataSourceProviders {

    private static Set<DataSourceProvider> ALL;

    public static void init(ModuleLayer layer) {
        if (ALL == null) {
            ALL = ServiceLoader.load(layer, DataSourceProvider.class).stream()
                    .map(ServiceLoader.Provider::get).collect(Collectors.toSet());
        }
    }

    public static Optional<DataSourceProvider> byDataSourceClass(Class<?> clazz) {
        if (ALL == null) {
            throw new IllegalStateException("Not initialized");
        }

        return ALL.stream().filter(d -> d.getType().equals(clazz)).findAny();
    }

    public static Optional<DataSourceProvider> byId(String name) {
        if (ALL == null) {
            throw new IllegalStateException("Not initialized");
        }

        return ALL.stream().filter(d -> d.getId().equals(name)).findAny();
    }

    public static Optional<DataSourceProvider> byFile(Path file) {
        if (ALL == null) {
            throw new IllegalStateException("Not initialized");
        }

        return ALL.stream().filter(d -> d.supportsFile(file)).findAny();
    }

    public static Set<DataSourceProvider> getAll() {
        return ALL;
    }
}
