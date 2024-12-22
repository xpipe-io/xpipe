package io.xpipe.app.core.mode;

import io.xpipe.app.comp.store.StoreViewState;
import io.xpipe.app.core.*;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.util.PlatformInit;
import io.xpipe.app.util.PlatformState;

public abstract class PlatformMode extends OperationMode {

    private boolean loaded;

    @Override
    public boolean isSupported() {
        return true;
    }

    @Override
    public void onSwitchTo() throws Throwable {
        OperationMode.BACKGROUND.onSwitchTo();
        if (loaded) {
            return;
        }
        PlatformInit.init(true);
        loaded = true;
    }

    @Override
    public void finalTeardown() throws Throwable {
        TrackEvent.info("Shutting down platform components");
        onSwitchFrom();
        StoreViewState.reset();
        AppLayoutModel.reset();
        AppTheme.reset();
        PlatformState.teardown();
        TrackEvent.info("Platform shutdown finished");
        BACKGROUND.finalTeardown();
    }
}
