package io.xpipe.extension;

import io.xpipe.core.data.type.TupleType;
import io.xpipe.core.source.TableDataSourceDescriptor;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.LocalFileDataStore;
import io.xpipe.extension.event.ErrorEvent;

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
    public static TableDataSourceDescriptor<LocalFileDataStore> createLocalTableDescriptor(TupleType type) {
        try {
            return (TableDataSourceDescriptor<LocalFileDataStore>)
                    DataSourceProviders.byId("xpbt").orElseThrow().getDescriptorClass()
                            .getDeclaredConstructors()[0].newInstance(type);
        } catch (Exception ex) {
            ErrorEvent.fromThrowable(ex).terminal(true).build().handle();
            return null;
        }
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

        return ALL.stream().filter(d -> d.getCliProvider() != null && d.getCliProvider().getPossibleNames().stream()
                .anyMatch(s -> s.equalsIgnoreCase(name))).findAny();
    }

    public static Optional<DataSourceProvider> byStore(DataStore store) {
        if (ALL == null) {
            throw new IllegalStateException("Not initialized");
        }

        return ALL.stream().filter(d -> d.getFileProvider() != null)
                .filter(d -> d.supportsStore(store)).findAny();
    }

    public static Set<DataSourceProvider> getAll() {
        return ALL;
    }
}
