package io.xpipe.extension;

import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;

public class SupportedApplicationProviders {

    private static Set<SupportedApplicationProvider> ALL;

    public static void loadAll(ModuleLayer layer) {
        if (ALL == null) {
            ALL = ServiceLoader.load(layer, SupportedApplicationProvider.class).stream()
                    .map(ServiceLoader.Provider::get)
                    .collect(Collectors.toSet());
        }
    }

    public static Optional<SupportedApplicationProvider> byId(String id) {
        if (ALL == null) {
            throw new IllegalStateException("Not initialized");
        }

        return ALL.stream().filter(d -> d.getId().equals(id)).findAny();
    }

    public static Set<SupportedApplicationProvider> getAll() {
        if (ALL == null) {
            throw new IllegalStateException("Not initialized");
        }

        return ALL;
    }
}
