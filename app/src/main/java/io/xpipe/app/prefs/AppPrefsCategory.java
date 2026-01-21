package io.xpipe.app.prefs;


import io.xpipe.app.platform.LabelGraphic;
import org.int4.fx.builders.common.AbstractRegionBuilder;
import io.xpipe.app.comp.BaseRegionBuilder;

public abstract class AppPrefsCategory {

    protected int getCompWidth() {
        return 600;
    }

    protected boolean show() {
        return true;
    }

    protected abstract String getId();

    protected abstract LabelGraphic getIcon();

    protected abstract BaseRegionBuilder<?,?> create();
}
