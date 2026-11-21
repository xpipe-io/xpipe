package io.xpipe.app.core;

import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.prefs.AppPrefs;

import javafx.stage.Screen;

import java.util.Comparator;
import java.util.stream.IntStream;

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

        var r = input - rest;
        if (r == 0) {
            return 25;
        } else {
            return r;
        }
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

    public static int getEffectiveIntegerDisplayScale() {
        var scale = (int) Math.round(getEffectiveDisplayScale() * 100);
        // mstsc only accepts these values
        return IntStream.of(100, 125, 150, 175, 200, 250, 300, 400, 500)
                .boxed()
                .min(Comparator.comparingInt(v -> Math.abs(v - scale)))
                .orElseThrow();
    }
}
