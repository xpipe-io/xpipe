package io.xpipe.app.core.mode;

import io.xpipe.app.comp.store.StoreViewState;
import io.xpipe.app.core.*;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.update.UpdateAvailableAlert;
import io.xpipe.app.util.PlatformState;
import io.xpipe.app.util.ThreadHelper;
import javafx.application.Application;

public abstract class PlatformMode extends OperationMode {

    @Override
    public boolean isSupported() {
        var r = PlatformState.initPlatformIfNeeded();
        return r;
    }

    @Override
    public void onSwitchTo() throws Throwable {
        if (App.getApp() != null) {
            return;
        }

        TrackEvent.info("mode", "Platform mode initial setup");
        PlatformState.initPlatformOrThrow();
        AppFont.init();
        AppTheme.init();
        AppStyle.init();
        AppImages.init();
        AppLayoutModel.init();
        TrackEvent.info("mode", "Finished essential component initialization before platform");

        TrackEvent.info("mode", "Launching application ...");
        ThreadHelper.createPlatformThread("app", false, () -> {
                    TrackEvent.info("mode", "Application thread started");
                    Application.launch(App.class);
                })
                .start();

        TrackEvent.info("mode", "Waiting for platform application startup ...");
        while (App.getApp() == null) {
            ThreadHelper.sleep(100);
        }
        TrackEvent.info("mode", "Application startup finished ...");

        // If we downloaded an update, and decided to no longer automatically update, don't remind us!
        // You can still update manually in the about tab
        if (AppPrefs.get().automaticallyUpdate().get()) {
            UpdateAvailableAlert.showIfNeeded();
        }

        StoreViewState.init();
    }

    @Override
    public void finalTeardown() throws Throwable {
        TrackEvent.info("mode", "Shutting down platform components");
        onSwitchFrom();
        StoreViewState.reset();
        AppLayoutModel.reset();
        PlatformState.teardown();
        TrackEvent.info("mode", "Platform shutdown finished");
        BACKGROUND.finalTeardown();
    }
}
