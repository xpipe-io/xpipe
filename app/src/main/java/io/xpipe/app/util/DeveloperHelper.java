package io.xpipe.app.util;

import io.xpipe.app.prefs.AppPrefs;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableBooleanValue;

public class DeveloperHelper {

    public static ObservableBooleanValue bindTrue(ObservableBooleanValue o) {
        return Bindings.createBooleanBinding(
                () -> {
                    return AppPrefs.get().developerMode().getValue() || o.get();
                },
                o,
                AppPrefs.get().developerMode());
    }

    public static ObservableBooleanValue bindFalls(ObservableBooleanValue o) {
        return Bindings.createBooleanBinding(
                () -> {
                    return !AppPrefs.get().developerMode().getValue() || o.get();
                },
                o,
                AppPrefs.get().developerMode());
    }
}
