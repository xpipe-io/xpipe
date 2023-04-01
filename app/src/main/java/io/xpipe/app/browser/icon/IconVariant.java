package io.xpipe.app.browser.icon;

import io.xpipe.app.prefs.AppPrefs;

public class IconVariant {

    private final String lightIcon;
    private final String darkIcon;

    public IconVariant(String icon) {
        this(icon, icon);
    }

    public IconVariant(String lightIcon, String darkIcon) {
        this.lightIcon = lightIcon;
        this.darkIcon = darkIcon;
    }

    protected final String getIcon() {
        var t = AppPrefs.get() != null ? AppPrefs.get().theme.getValue() : null;
        if (t == null) {
            return lightIcon;
        }

        return t.getTheme().isDarkMode() ? darkIcon : lightIcon;
    }
}
