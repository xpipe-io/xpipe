package io.xpipe.app.core;

import io.xpipe.app.core.window.AppMainWindow;
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

            var s = AppPrefs.get() != null ? AppPrefs.get().uiScale().getValue() : null;
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

    public static Integer clampValue(Integer input) {
        if (input == null) {
            return null;
        }

        var rest = input % 25;
        if (rest == 0) {
            return input;
        }

        return input - rest;
    }

    public static boolean hasOnlyDefaultDisplayScale() {
        return defaultDisplayScale != null ? defaultDisplayScale : false;
    }

    public static double getEffectiveDisplayScale() {
        if (AppPrefs.get() != null) {
            var s = AppPrefs.get().uiScale().getValue();
            if (s != null) {
                var i = Math.clamp(s, 25, 300);
                var percent = i / 100.0;
                return percent;
            }
        }

        return screenOutputScale != null ? screenOutputScale : 1.0;
    }
}
