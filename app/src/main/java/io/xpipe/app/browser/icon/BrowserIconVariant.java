package io.xpipe.app.browser.icon;

import io.xpipe.app.prefs.AppPrefs;

public class BrowserIconVariant {

    private final String lightIcon;
    private final String darkIcon;

    public BrowserIconVariant(String lightIcon, String darkIcon) {
        this.lightIcon = lightIcon;
        this.darkIcon = darkIcon;
    }

    protected final String getIcon() {
        var t = AppPrefs.get() != null ? AppPrefs.get().theme().getValue() : null;
        if (t == null) {
            return lightIcon;
        }

        return t.isDark() ? darkIcon : lightIcon;
    }
}
