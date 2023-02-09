package io.xpipe.extension;

import io.xpipe.core.impl.FileStore;
import io.xpipe.core.source.*;
import io.xpipe.core.store.DataStore;
import io.xpipe.extension.event.ErrorEvent;
import lombok.SneakyThrows;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

public class DataSourceProviders {

    private static List<DataSourceProvider<?>> ALL;

    public static void init(ModuleLayer layer) {
        if (ALL == null) {
            ALL = ServiceLoader.load(layer, DataSourceProvider.class).stream()
                    .map(p -> (DataSourceProvider<?>) p.get())
                    .sorted(Comparator.comparing(DataSourceProvider::getId))
                    .collect(Collectors.toList());
            ALL.removeIf(p -> {
                try {
                    p.init();
                    p.validate();
                    return false;
                } catch (Throwable e) {
                    ErrorEvent.fromThrowable(e).handle();
                    return true;
                }
            });
        }
    }

    public static DataSourceProvider<?> getInternalProviderForType(DataSourceType t) {
        try {
            return switch (t) {
                case TABLE -> DataSourceProviders.byId("xpbt");
                case STRUCTURE -> DataSourceProviders.byId("xpbs");
                case TEXT -> DataSourceProviders.byId("text");
                case RAW -> DataSourceProviders.byId("binary");
                    // TODO
                case COLLECTION -> null;
            };
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    public static StructureDataSource<FileStore> createLocalStructureDescriptor(DataStore store) {
        return (StructureDataSource<FileStore>) DataSourceProviders.byId("xpbs")
                .getSourceClass()
                .getDeclaredConstructors()[0]
                .newInstance(store);
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    public static RawDataSource<FileStore> createLocalRawDescriptor(DataStore store) {
        return (RawDataSource<FileStore>) DataSourceProviders.byId("binary")
                .getSourceClass()
                .getDeclaredConstructors()[0]
                .newInstance(store);
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    public static RawDataSource<FileStore> createLocalCollectionDescriptor(DataStore store) {
        return (RawDataSource<FileStore>) DataSourceProviders.byId("br")
                .getSourceClass()
                .getDeclaredConstructors()[0]
                .newInstance(store);
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    public static TextDataSource<FileStore> createLocalTextDescriptor(DataStore store) {
        return (TextDataSource<FileStore>) DataSourceProviders.byId("text")
                .getSourceClass()
                .getDeclaredConstructors()[0]
                .newInstance(store);
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    public static TableDataSource<FileStore> createLocalTableDescriptor(DataStore store) {
        return (TableDataSource<FileStore>) DataSourceProviders.byId("xpbt")
                .getSourceClass()
                .getDeclaredConstructors()[0]
                .newInstance(store);
    }

    @SuppressWarnings("unchecked")
    public static <T extends DataSourceProvider<?>> T byId(String name) {
        if (ALL == null) {
            throw new IllegalStateException("Not initialized");
        }

        return (T) ALL.stream()
                .filter(d -> d.getId().equals(name))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("Provider " + name + " not found"));
    }

    @SuppressWarnings("unchecked")
    public static <C extends DataSource<?>, T extends DataSourceProvider<C>> T byDataSourceClass(Class<C> c) {
        if (ALL == null) {
            throw new IllegalStateException("Not initialized");
        }

        return (T) ALL.stream()
                .filter(d -> d.getSourceClass().equals(c))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("Provider for " + c.getSimpleName() + " not found"));
    }

    public static Optional<DataSourceProvider<?>> byName(String name) {
        if (ALL == null) {
            throw new IllegalStateException("Not initialized");
        }

        return ALL.stream()
                .filter(d -> d.getPossibleNames().stream()
                                .anyMatch(s -> nameAlternatives(s).stream().anyMatch(s1 -> s1.equalsIgnoreCase(name)))
                        || d.getId().equalsIgnoreCase(name))
                .findAny();
    }

    private static List<String> nameAlternatives(String name) {
        var split = List.of(name.split("_"));
        return List.of(
                String.join(" ", split),
                String.join("_", split),
                String.join("-", split),
                split.stream()
                        .map(s -> s.equals(split.get(0)) ? s : s.substring(0, 1).toUpperCase() + s.substring(1))
                        .collect(Collectors.joining()));
    }

    public static Optional<DataSourceProvider<?>> byPreferredStore(DataStore store, DataSourceType type) {
        if (ALL == null) {
            throw new IllegalStateException("Not initialized");
        }

        return ALL.stream()
                .filter(d -> type == null || d.getPrimaryType() == type)
                .filter(d -> d.getFileProvider() != null)
                .filter(d -> d.prefersStore(store, type))
                .findAny();
    }

    public static List<DataSourceProvider<?>> getAll() {
        return ALL;
    }
}
