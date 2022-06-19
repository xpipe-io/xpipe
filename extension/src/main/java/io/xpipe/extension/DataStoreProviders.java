package io.xpipe.extension;

import io.xpipe.core.dialog.Dialog;
import io.xpipe.extension.event.ErrorEvent;

import java.net.URL;
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
            ALL.forEach(p -> {
                try {
                    p.init();
                } catch (Exception e) {
                    ErrorEvent.fromThrowable(e).handle();
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

    public static Optional<Dialog> byURL(URL url) {
        if (ALL == null) {
            throw new IllegalStateException("Not initialized");
        }

        return ALL.stream().map(d -> d.dialogForURL(url)).filter(Objects::nonNull).findAny();
    }

    public static Optional<Dialog> byString(String s) {
        if (ALL == null) {
            throw new IllegalStateException("Not initialized");
        }

        return ALL.stream().map(d -> d.dialogForString(s)).filter(Objects::nonNull).findAny();
    }

    @SuppressWarnings("unchecked")
    public static <T extends DataStoreProvider> T byStoreClass(Class<?> c) {
        if (ALL == null) {
            throw new IllegalStateException("Not initialized");
        }

        return (T) ALL.stream().filter(d -> d.getStoreClasses().contains(c)).findAny()
                .orElseThrow(() -> new IllegalArgumentException("Provider for " + c.getSimpleName() + " not found"));
    }

    public static Set<DataStoreProvider> getAll() {
        return ALL;
    }
}
