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

    public static Optional<DataStoreProvider> byName(String name) {
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

    public static Optional<Dialog> byString(String s) {
        if (ALL == null) {
            throw new IllegalStateException("Not initialized");
        }

        return ALL.stream()
                .map(d -> {
                    var store = d.storeForString(s);
                    if (store != null) {
                        return d.dialogForStore(store);
                    } else {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .findAny();
    }

    @SuppressWarnings("unchecked")
    public static <T extends DataStoreProvider> T byStore(DataStore store) {
        return (T) byStoreClass(store.getClass()).orElseThrow();
    }

    @SuppressWarnings("unchecked")
    public static <T extends DataStoreProvider> Optional<T> byStoreClass(Class<?> c) {
        if (ALL == null) {
            throw new IllegalStateException("Not initialized");
        }

        return (Optional<T>)
                ALL.stream().filter(d -> d.getStoreClasses().contains(c)).findAny();
    }

    public static List<DataStoreProvider> getAll() {
        return ALL;
    }
}
