package io.xpipe.app.core;

public class AppMacros {

    private static AppMacros INSTANCE;

    public static void init() {
        INSTANCE = new AppMacros();
    }

    public static AppMacros get() {
        return INSTANCE;
    }

    public boolean isMacroSelectionModeEnabled() {
        return true;
    }
}
