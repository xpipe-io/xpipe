package io.xpipe.app.ext;

import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.util.JacksonMapper;
import io.xpipe.core.util.ModuleLayerLoader;

import com.fasterxml.jackson.databind.jsontype.NamedType;

import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

public class DataStoreProviders {

    private static List<DataStoreProvider> ALL;

    public static void init() {
        DataStoreProviders.getAll().forEach(dataStoreProvider -> {
            try {
                dataStoreProvider.init();
            } catch (Exception e) {
                ErrorEvent.fromThrowable(e).omit().handle();
            }
        });
    }

    public static void reset() {
        DataStoreProviders.getAll().forEach(dataStoreProvider -> {
            try {
                dataStoreProvider.reset();
            } catch (Exception e) {
                ErrorEvent.fromThrowable(e).omit().handle();
            }
        });
    }

    public static Optional<DataStoreProvider> byId(String id) {
        if (ALL == null) {
            throw new IllegalStateException("Not initialized");
        }

        return ALL.stream().filter(d -> d.getId().equalsIgnoreCase(id)).findAny();
    }

    @SuppressWarnings("unchecked")
    public static <T extends DataStoreProvider> T byStore(DataStore store) {
        if (ALL == null) {
            throw new IllegalStateException("Not initialized");
        }

        return (T) ALL.stream()
                .filter(d -> d.getStoreClasses().contains(store.getClass()))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("Unknown store class"));
    }

    public static List<DataStoreProvider> getAll() {
        return ALL;
    }

    public static class Loader implements ModuleLayerLoader {

        @Override
        public void init(ModuleLayer layer) {
            TrackEvent.info("Loading extension providers ...");
            ALL = ServiceLoader.load(layer, DataStoreProvider.class).stream()
                    .map(ServiceLoader.Provider::get)
                    .collect(Collectors.toList());
            ALL.removeIf(p -> {
                try {
                    if (!p.preInit()) {
                        return true;
                    }

                    p.validate();
                    return false;
                } catch (Throwable e) {
                    ErrorEvent.fromThrowable(e).handle();
                    return true;
                }
            });

            for (DataStoreProvider p : getAll()) {
                TrackEvent.trace("Loaded data store provider " + p.getId());
                JacksonMapper.configure(objectMapper -> {
                    for (Class<?> storeClass : p.getStoreClasses()) {
                        objectMapper.registerSubtypes(new NamedType(storeClass));
                    }
                });
            }
        }
    }
}
