package io.xpipe.app.core;

import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.prefs.AppPrefs;

import javafx.stage.Screen;

public class AppDisplayScale {

    private static Double screenOutputScale;
    private static Boolean defaultDisplayScale;

    public static void init() {
        try {
            Screen primary = Screen.getPrimary();
            if (primary != null) {
                screenOutputScale = primary.getOutputScaleX();
            }

            var s = AppPrefs.get().uiScale().getValue();
            if (s != null && s == 100) {
                defaultDisplayScale = true;
            }

            var allScreensDefault = Screen.getScreens().stream().allMatch(screen -> screen.getOutputScaleX() == 1.0);
            if (allScreensDefault) {
                defaultDisplayScale = true;
            }
        } catch (Exception e) {
            ErrorEventFactory.fromThrowable(e).omit().expected().handle();
        }
    }

    public static boolean hasOnlyDefaultDisplayScale() {
        return defaultDisplayScale != null ? defaultDisplayScale : false;
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

        return screenOutputScale != null ? screenOutputScale : 1.0;
    }
}
