package io.xpipe.app.ext;

import io.xpipe.core.ModuleLayerLoader;

import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

public abstract class PrefsProvider {

    private static List<PrefsProvider> ALL;

    public static List<PrefsProvider> getAll() {
        return ALL;
    }

    @SuppressWarnings("unchecked")
    public static <T extends PrefsProvider> T get(Class<T> c) {
        return (T) ALL.stream()
                .filter(prefsProvider -> prefsProvider.getClass().equals(c))
                .findAny()
                .orElseThrow();
    }

    public abstract void addPrefs(PrefsHandler handler);

    public abstract void fixLocalValues();

    public abstract void initDefaultValues();

    public static class Loader implements ModuleLayerLoader {

        @Override
        public void init(ModuleLayer layer) {
            ALL = ServiceLoader.load(layer, PrefsProvider.class).stream()
                    .map(ServiceLoader.Provider::get)
                    .collect(Collectors.toList());
        }
    }
}
