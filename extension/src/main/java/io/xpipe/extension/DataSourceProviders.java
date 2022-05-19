package io.xpipe.extension;

import io.xpipe.core.source.*;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.LocalFileDataStore;
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
            ALL.forEach(DataSourceProvider::init);
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
    public static StructureDataSourceDescriptor<LocalFileDataStore> createLocalStructureDescriptor(DataStore store) {
        return (StructureDataSourceDescriptor<LocalFileDataStore>)
                DataSourceProviders.byId("xpbs").getDescriptorClass()
                        .getDeclaredConstructors()[0].newInstance(store);
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    public static RawDataSourceDescriptor<LocalFileDataStore> createLocalRawDescriptor(DataStore store) {
        return (RawDataSourceDescriptor<LocalFileDataStore>)
                DataSourceProviders.byId("binary").getDescriptorClass()
                        .getDeclaredConstructors()[0].newInstance(store);
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    public static RawDataSourceDescriptor<LocalFileDataStore> createLocalCollectionDescriptor(DataStore store) {
        return (RawDataSourceDescriptor<LocalFileDataStore>)
                DataSourceProviders.byId("br").getDescriptorClass()
                        .getDeclaredConstructors()[0].newInstance(store);
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    public static TextDataSourceDescriptor<LocalFileDataStore> createLocalTextDescriptor(DataStore store) {
        return (TextDataSourceDescriptor<LocalFileDataStore>)
                DataSourceProviders.byId("text").getDescriptorClass()
                        .getDeclaredConstructors()[0].newInstance(store);
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    public static TableDataSourceDescriptor<LocalFileDataStore> createLocalTableDescriptor(DataStore store) {
        return (TableDataSourceDescriptor<LocalFileDataStore>)
                DataSourceProviders.byId("xpbt").getDescriptorClass()
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
    public static <C extends DataSourceDescriptor<?>, T extends DataSourceProvider<C>> T byDataSourceClass(Class<C> c) {
        if (ALL == null) {
            throw new IllegalStateException("Not initialized");
        }

        return (T) ALL.stream().filter(d -> d.getDescriptorClass().equals(c)).findAny()
                .orElseThrow(() -> new IllegalArgumentException("Provider for " + c.getSimpleName() + " not found"));
    }

    public static Optional<DataSourceProvider<?>> byName(String name) {
        if (ALL == null) {
            throw new IllegalStateException("Not initialized");
        }

        return ALL.stream().filter(d -> d.getConfigProvider().getPossibleNames().stream()
                .anyMatch(s -> s.equalsIgnoreCase(name))).findAny();
    }

    public static Optional<DataSourceProvider<?>> byStore(DataStore store) {
        if (ALL == null) {
            throw new IllegalStateException("Not initialized");
        }

        return ALL.stream().filter(d -> d.getFileProvider() != null)
                .filter(d -> d.couldSupportStore(store)).findAny();
    }

    public static Set<DataSourceProvider<?>> getAll() {
        return ALL;
    }
}
