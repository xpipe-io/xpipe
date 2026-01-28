package io.xpipe.app.core;

import io.xpipe.app.prefs.AppPrefs;

import javafx.stage.Screen;

public class AppDisplayScale {

    private static Double screenOutputScale;

    public static boolean hasDefaultDisplayScale() {
        return getEffectiveDisplayScale() == 1.0;
    }

    public static double getEffectiveDisplayScale() {
        if (AppPrefs.get() != null) {
            var s = AppPrefs.get().uiScale().getValue();
            if (s != null) {
                var i = Math.min(300, Math.max(25, s));
                var percent = i / 100.0;
                return percent;
            }
        }

        if (screenOutputScale == null) {
            screenOutputScale = Screen.getPrimary().getOutputScaleX();
        }
        return screenOutputScale;
    }
}
