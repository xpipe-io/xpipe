package io.xpipe.app.prefs;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.util.LabelGraphic;

public abstract class AppPrefsCategory {

    protected int getCompWidth() {
        return 600;
    }

    protected boolean show() {
        return true;
    }

    protected abstract String getId();

    protected abstract LabelGraphic getIcon();

    protected abstract Comp<?> create();
}
