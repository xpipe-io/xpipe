package io.xpipe.app.core.mode;

import io.xpipe.app.comp.store.StoreViewState;
import io.xpipe.app.core.*;
import io.xpipe.app.core.check.AppGpuCheck;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.resources.AppImages;
import io.xpipe.app.update.UpdateAvailableAlert;
import io.xpipe.app.util.PlatformInit;
import io.xpipe.app.util.PlatformState;
import io.xpipe.app.util.ThreadHelper;

import javafx.application.Application;

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

        TrackEvent.info("Platform mode initial setup");
        PlatformInit.init(true);
        loaded = true;
        TrackEvent.info("Platform mode startup finished");
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
