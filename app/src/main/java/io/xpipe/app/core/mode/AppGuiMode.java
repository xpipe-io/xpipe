package io.xpipe.app.core.mode;

import io.xpipe.app.core.window.AppDialog;
import io.xpipe.app.core.window.AppMainWindow;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.platform.PlatformInit;
import io.xpipe.app.platform.PlatformThread;
import io.xpipe.app.util.LicenseProvider;

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
        TrackEvent.info("Closing windows");
        PlatformThread.runLaterIfNeededBlocking(() -> {
            // Close dialogs
            AppDialog.getModalOverlays().clear();

            // Close other windows
            Stage.getWindows().stream()
                    .filter(w -> !w.equals(AppMainWindow.get().getStage()))
                    .toList()
                    .forEach(w -> w.hide());

            // When changing between modes, close window instantly
            // Otherwise, the background mode shutdown closes this
            if (!AppOperationMode.isInShutdown()) {
                AppMainWindow.get().hide();
            } else {
                // Show teardown screen
                AppMainWindow.resetContent();
            }
        });
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
