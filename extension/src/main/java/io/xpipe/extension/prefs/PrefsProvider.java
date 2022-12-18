package io.xpipe.extension.prefs;

import com.dlsc.formsfx.model.structure.Field;
import javafx.beans.value.ObservableBooleanValue;

import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class PrefsProvider {

    protected <T extends Field<?>> T editable(T o, ObservableBooleanValue v) {
        o.editableProperty().bind(v);
        return o;
    }

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

    @SuppressWarnings("unchecked")
    public static <T extends PrefsProvider> T get(Class<T> c) {
        return (T) ALL.stream().filter(prefsProvider -> prefsProvider.getClass().equals(c)).findAny().orElseThrow();
    }

    public abstract void addPrefs(PrefsHandler handler);
}
