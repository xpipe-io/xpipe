package io.xpipe.extension;

import io.xpipe.core.dialog.Dialog;
import io.xpipe.core.store.DataStore;
import io.xpipe.extension.event.ErrorEvent;

import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;

public class DataStoreProviders {

    private static Set<DataStoreProvider> ALL;

    public static void init(ModuleLayer layer) {
        if (ALL == null) {
            ALL = ServiceLoader.load(layer, DataStoreProvider.class).stream()
                    .map(ServiceLoader.Provider::get).collect(Collectors.toSet());
            ALL.removeIf(p -> {
                try {
                    return !p.init();
                } catch (Exception e) {
                    ErrorEvent.fromThrowable(e).handle();
                    return true;
                }
            });
        }
    }

    public static Optional<DataStoreProvider> byName(String name) {
        if (ALL == null) {
            throw new IllegalStateException("Not initialized");
        }

        return ALL.stream().filter(d -> d.getPossibleNames().stream()
                .anyMatch(s -> s.equalsIgnoreCase(name))).findAny();
    }


    public static Optional<Dialog> byString(String s) {
        if (ALL == null) {
            throw new IllegalStateException("Not initialized");
        }

        return ALL.stream().map(d -> {
                    var store = d.storeForString(s);
                    if (store != null) {
                        return d.dialogForStore(store);
                    } else {
                        return null;
                    }
                }
        ).filter(Objects::nonNull).findAny();
    }


    public static <T extends DataStoreProvider> T byStore(DataStore store) {
        return (T) byStoreClass(store.getClass()).orElseThrow(() -> new IllegalArgumentException("Provider for " + store.getClass().getSimpleName() + " not found"));
    }

    @SuppressWarnings("unchecked")
    public static <T extends DataStoreProvider> Optional<T> byStoreClass(Class<?> c) {
        if (ALL == null) {
            throw new IllegalStateException("Not initialized");
        }

        return (Optional<T>) ALL.stream().filter(d -> d.getStoreClasses().contains(c)).findAny();
    }

    public static Set<DataStoreProvider> getAll() {
        return ALL;
    }
}
