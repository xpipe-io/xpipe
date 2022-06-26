package io.xpipe.extension;

import io.xpipe.core.source.*;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.FileStore;
import io.xpipe.extension.event.ErrorEvent;
import lombok.SneakyThrows;

import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;

public class DataSourceProviders {

    private static Set<DataSourceProvider<?>> ALL;

    public static void init(ModuleLayer layer) {
        if (ALL == null) {
            ALL = ServiceLoader.load(layer, DataSourceProvider.class).stream()
                    .map(p -> (DataSourceProvider<?>) p.get()).collect(Collectors.toSet());
            ALL.removeIf(p -> {
                try {
                    p.init();
                    p.validate();
                    return false;
                } catch (Exception e) {
                    ErrorEvent.fromThrowable(e).handle();
                    return true;
                }
            });
        }
    }

    public static DataSourceProvider<?> getNativeDataSourceDescriptorForType(DataSourceType t) {
        try {
            return switch (t) {
                case TABLE -> DataSourceProviders.byId("xpbt");
                case STRUCTURE -> DataSourceProviders.byId("xpbs");
                case TEXT -> DataSourceProviders.byId("text");
                case RAW -> DataSourceProviders.byId("binary");
                //TODO
                case COLLECTION -> null;
            };
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    public static StructureDataSource<FileStore> createLocalStructureDescriptor(DataStore store) {
        return (StructureDataSource<FileStore>)
                DataSourceProviders.byId("xpbs").getSourceClass()
                        .getDeclaredConstructors()[0].newInstance(store);
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    public static RawDataSource<FileStore> createLocalRawDescriptor(DataStore store) {
        return (RawDataSource<FileStore>)
                DataSourceProviders.byId("binary").getSourceClass()
                        .getDeclaredConstructors()[0].newInstance(store);
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    public static RawDataSource<FileStore> createLocalCollectionDescriptor(DataStore store) {
        return (RawDataSource<FileStore>)
                DataSourceProviders.byId("br").getSourceClass()
                        .getDeclaredConstructors()[0].newInstance(store);
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    public static TextDataSource<FileStore> createLocalTextDescriptor(DataStore store) {
        return (TextDataSource<FileStore>)
                DataSourceProviders.byId("text").getSourceClass()
                        .getDeclaredConstructors()[0].newInstance(store);
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    public static TableDataSource<FileStore> createLocalTableDescriptor(DataStore store) {
        return (TableDataSource<FileStore>)
                DataSourceProviders.byId("xpbt").getSourceClass()
                        .getDeclaredConstructors()[0].newInstance(store);
    }

    @SuppressWarnings("unchecked")
    public static <T extends DataSourceProvider<?>> T byId(String name) {
        if (ALL == null) {
            throw new IllegalStateException("Not initialized");
        }

        return (T) ALL.stream().filter(d -> d.getId().equals(name)).findAny()
                .orElseThrow(() -> new IllegalArgumentException("Provider " + name + " not found"));
    }


    @SuppressWarnings("unchecked")
    public static <C extends DataSource<?>, T extends DataSourceProvider<C>> T byDataSourceClass(Class<C> c) {
        if (ALL == null) {
            throw new IllegalStateException("Not initialized");
        }

        return (T) ALL.stream().filter(d -> d.getSourceClass().equals(c)).findAny()
                .orElseThrow(() -> new IllegalArgumentException("Provider for " + c.getSimpleName() + " not found"));
    }

    public static Optional<DataSourceProvider<?>> byName(String name) {
        if (ALL == null) {
            throw new IllegalStateException("Not initialized");
        }

        return ALL.stream().filter(d -> d.getPossibleNames().stream()
                .anyMatch(s -> s.equalsIgnoreCase(name))).findAny();
    }

    public static Optional<DataSourceProvider<?>> byPreferredStore(DataStore store) {
        if (ALL == null) {
            throw new IllegalStateException("Not initialized");
        }

        return ALL.stream().filter(d -> d.getFileProvider() != null)
                .filter(d -> d.prefersStore(store)).findAny();
    }

    public static Set<DataSourceProvider<?>> getAll() {
        return ALL;
    }
}
