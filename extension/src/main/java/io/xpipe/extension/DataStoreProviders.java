package io.xpipe.extension;

import io.xpipe.core.dialog.Dialog;
import io.xpipe.core.store.DataStore;
import io.xpipe.extension.event.ErrorEvent;

import java.util.*;
import java.util.stream.Collectors;

public class DataStoreProviders {

    private static List<DataStoreProvider> ALL;

    public static void init(ModuleLayer layer) {
        if (ALL == null) {
            ALL = ServiceLoader.load(layer, DataStoreProvider.class).stream()
                    .map(ServiceLoader.Provider::get)
                    .sorted(Comparator.comparing(DataStoreProvider::getId))
                    .collect(Collectors.toList());
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

    @SuppressWarnings("unchecked")
    public static <T extends DataStoreProvider> T byStore(DataStore store) {
        return (T) byStoreClass(store.getClass());
    }

    @SuppressWarnings("unchecked")
    public static <T extends DataStoreProvider> T byStoreClass(Class<?> c) {
        if (ALL == null) {
            throw new IllegalStateException("Not initialized");
        }


        return (T) ALL.stream().filter(d -> d.getStoreClasses().contains(c)).findAny().orElseThrow();
    }

    public static List<DataStoreProvider> getAll() {
        return ALL;
    }
}
