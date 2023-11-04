package io.xpipe.app.ext;

import com.dlsc.formsfx.model.structure.Field;
import io.xpipe.core.util.ModuleLayerLoader;
import javafx.beans.value.ObservableBooleanValue;

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
        return (T) ALL.stream().filter(prefsProvider -> prefsProvider.getClass().equals(c)).findAny().orElseThrow();
    }

    protected <T extends Field<?>> T editable(T o, ObservableBooleanValue v) {
        o.editableProperty().bind(v);
        return o;
    }

    public abstract void addPrefs(PrefsHandler handler);

    public abstract void init();

    public static class Loader implements ModuleLayerLoader {

        @Override
        public void init(ModuleLayer layer) {
            ALL = ServiceLoader.load(layer, PrefsProvider.class).stream().map(ServiceLoader.Provider::get).collect(Collectors.toList());
        }

        @Override
        public boolean requiresFullDaemon() {
            return true;
        }

        @Override
        public boolean prioritizeLoading() {
            return false;
        }
    }
}
