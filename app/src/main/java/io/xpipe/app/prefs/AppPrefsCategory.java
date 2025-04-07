package io.xpipe.app.prefs;

import io.xpipe.app.comp.Comp;

public abstract class AppPrefsCategory {

    protected int getCompWidth() {
        return 600;
    }

    protected boolean show() {
        return true;
    }

    protected abstract String getId();

    protected abstract Comp<?> create();
}
