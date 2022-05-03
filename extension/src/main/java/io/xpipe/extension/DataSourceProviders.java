package io.xpipe.extension;

import io.xpipe.core.data.type.TupleType;
import io.xpipe.core.source.*;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.LocalFileDataStore;
import io.xpipe.core.store.StreamDataStore;
import lombok.SneakyThrows;

import java.nio.charset.StandardCharsets;
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

    @SuppressWarnings("unchecked")
    public static DataSourceDescriptor<StreamDataStore> getNativeDataSourceDescriptorForType(DataSourceType t) {
        try {
            return switch (t) {
                case TABLE -> (DataSourceDescriptor<StreamDataStore>) DataSourceProviders.byId("xpbt").orElseThrow()
                        .getDescriptorClass().getConstructors()[0].newInstance();
                case STRUCTURE -> (DataSourceDescriptor<StreamDataStore>) DataSourceProviders.byId("xpbs").orElseThrow()
                        .getDescriptorClass().getConstructors()[0].newInstance();
                case TEXT -> (DataSourceDescriptor<StreamDataStore>) DataSourceProviders.byId("text").orElseThrow()
                        .getDescriptorClass().getConstructors()[0].newInstance(StandardCharsets.UTF_8);
                case RAW -> (DataSourceDescriptor<StreamDataStore>) DataSourceProviders.byId("xpbr").orElseThrow()
                        .getDescriptorClass().getConstructors()[0].newInstance();
            };
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    public static StructureDataSourceDescriptor<LocalFileDataStore> createLocalStructureDescriptor() {
        return (StructureDataSourceDescriptor<LocalFileDataStore>)
                DataSourceProviders.byId("json").orElseThrow().getDescriptorClass()
                        .getDeclaredConstructors()[0].newInstance();
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    public static RawDataSourceDescriptor<LocalFileDataStore> createLocalRawDescriptor() {
        return (RawDataSourceDescriptor<LocalFileDataStore>)
                DataSourceProviders.byId("binary").orElseThrow().getDescriptorClass()
                        .getDeclaredConstructors()[0].newInstance();
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    public static TextDataSourceDescriptor<LocalFileDataStore> createLocalTextDescriptor() {
        return (TextDataSourceDescriptor<LocalFileDataStore>)
                DataSourceProviders.byId("text").orElseThrow().getDescriptorClass()
                        .getDeclaredConstructors()[0].newInstance();
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    public static TableDataSourceDescriptor<LocalFileDataStore> createLocalTableDescriptor(TupleType type) {
        return (TableDataSourceDescriptor<LocalFileDataStore>)
                DataSourceProviders.byId("xpbt").orElseThrow().getDescriptorClass()
                        .getDeclaredConstructors()[0].newInstance(type);
    }

    public static Optional<DataSourceProvider> byDescriptorClass(Class<?> clazz) {
        if (ALL == null) {
            throw new IllegalStateException("Not initialized");
        }

        return ALL.stream().filter(d -> d.getDescriptorClass().equals(clazz)).findAny();
    }

    public static Optional<DataSourceProvider> byId(String name) {
        if (ALL == null) {
            throw new IllegalStateException("Not initialized");
        }

        return ALL.stream().filter(d -> d.getId().equals(name)).findAny();
    }

    public static Optional<DataSourceProvider> byName(String name) {
        if (ALL == null) {
            throw new IllegalStateException("Not initialized");
        }

        return ALL.stream().filter(d -> d.getConfigProvider().getPossibleNames().stream()
                .anyMatch(s -> s.equalsIgnoreCase(name))).findAny();
    }

    public static Optional<DataSourceProvider> byStore(DataStore store) {
        if (ALL == null) {
            throw new IllegalStateException("Not initialized");
        }

        return ALL.stream().filter(d -> d.getFileProvider() != null)
                .filter(d -> d.couldSupportStore(store)).findAny();
    }

    public static Set<DataSourceProvider> getAll() {
        return ALL;
    }
}
