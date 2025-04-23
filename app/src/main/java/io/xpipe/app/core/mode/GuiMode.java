package io.xpipe.app.core.mode;

import io.xpipe.app.core.window.AppMainWindow;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.util.PlatformThread;

import javafx.application.Platform;
import javafx.stage.Stage;

public class GuiMode extends PlatformMode {

    @Override
    public String getId() {
        return "gui";
    }

    @Override
    public void onSwitchFrom() {
        // If we are in an externally started shutdown hook, don't close the windows until the platform exits
        // That way, it is kept open to block for shutdowns
        if (!OperationMode.isInShutdownHook()) {
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
        super.onSwitchTo();
        PlatformThread.runLaterIfNeededBlocking(() -> {
            AppMainWindow.getInstance().show();
        });
    }
}
