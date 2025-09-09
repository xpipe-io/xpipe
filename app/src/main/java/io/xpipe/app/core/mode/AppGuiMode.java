package io.xpipe.app.core.mode;

import io.xpipe.app.core.window.AppMainWindow;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.platform.PlatformInit;
import io.xpipe.app.platform.PlatformThread;
import io.xpipe.app.util.LicenseProvider;
import io.xpipe.core.OsType;

import javafx.application.Platform;
import javafx.stage.Stage;

public class AppGuiMode extends AppOperationMode {

    @Override
    public boolean isSupported() {
        // We force GUI to be supported and fail with a terminal
        // exception if we can't initialize the platform
        return true;
    }

    @Override
    public String getId() {
        return "gui";
    }

    @Override
    public void onSwitchFrom() {
        // If we are in an externally started shutdown hook, don't close the windows until the platform exits
        // That way, it is kept open to block for shutdowns on Windows systems
        if (OsType.getLocal() != OsType.WINDOWS || !AppOperationMode.isInShutdownHook()) {
            Platform.runLater(() -> {
                TrackEvent.info("Closing windows");
                Stage.getWindows().stream().toList().forEach(w -> {
                    w.hide();
                });
            });
        }
    }

    @Override
    public void onSwitchTo() throws Throwable {
        AppOperationMode.BACKGROUND.onSwitchTo();
        PlatformInit.init(true);

        // Refresh license check
        // In case our exit behavior is set to continue in background,
        // this will apply a new license properly
        LicenseProvider.get().init();

        PlatformThread.runLaterIfNeededBlocking(() -> {
            AppMainWindow.get().show();
        });
    }

    @Override
    public void finalTeardown() throws Throwable {
        onSwitchFrom();
        BACKGROUND.finalTeardown();
    }
}
