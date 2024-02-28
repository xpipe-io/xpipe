package io.xpipe.app.ext;

import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.core.store.DataStore;

import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

public class DataStoreProviders {

    private static List<DataStoreProvider> ALL;

    public static void init(ModuleLayer layer) {
        if (ALL == null) {
            ALL = ServiceLoader.load(layer, DataStoreProvider.class).stream()
                    .map(ServiceLoader.Provider::get)
                    .collect(Collectors.toList());
            ALL.removeIf(p -> {
                try {
                    if (!p.init()) {
                        return true;
                    }

                    p.validate();
                    return false;
                } catch (Throwable e) {
                    ErrorEvent.fromThrowable(e).handle();
                    return true;
                }
            });
        }
    }

    public static void postInit(ModuleLayer layer) {
        ALL.forEach(p -> {
            try {
                p.postInit();
            } catch (Throwable e) {
                ErrorEvent.fromThrowable(e).handle();
            }
        });
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
                        .map(s -> s.equals(split.getFirst())
                                ? s
                                : s.substring(0, 1).toUpperCase() + s.substring(1))
                        .collect(Collectors.joining()));
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
