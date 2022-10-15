package io.xpipe.extension.prefs;

import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;

public class PrefsProviders {

    private static Set<PrefsProvider> ALL;

    public static void init(ModuleLayer layer) {
        if (ALL == null) {
            ALL = ServiceLoader.load(layer, PrefsProvider.class).stream()
                    .map(ServiceLoader.Provider::get)
                    .collect(Collectors.toSet());
        }
    }

    public static Set<PrefsProvider> getAll() {
        return ALL;
    }
}
