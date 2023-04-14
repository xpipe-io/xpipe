package io.xpipe.app.core.mode;

import io.xpipe.app.core.App;
import io.xpipe.app.core.AppGreetings;
import io.xpipe.app.issue.*;
import io.xpipe.app.update.UpdateChangelogAlert;
import io.xpipe.app.util.PlatformState;
import javafx.application.Platform;

import java.util.concurrent.CountDownLatch;

public class GuiMode extends PlatformMode {

    @Override
    public String getId() {
        return "gui";
    }

    @Override
    public void onSwitchTo() {
        if (PlatformState.getCurrent() == PlatformState.NOT_INITIALIZED) {
            super.platformSetup();
        }

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                TrackEvent.info("mode", "Setting up window ...");
                App.getApp().setupWindow();
                AppGreetings.showIfNeeded();
                UpdateChangelogAlert.showIfNeeded();
                latch.countDown();
            } catch (Throwable t) {
                ErrorEvent.fromThrowable(t).terminal(true).handle();
            }
        });

        TrackEvent.info("mode", "Waiting for window setup completion ...");
        try {
            latch.await();
        } catch (InterruptedException ignored) {
        }
        TrackEvent.info("mode", "Window setup complete");
    }

    @Override
    public void onSwitchFrom() {
        if (PlatformState.getCurrent() == PlatformState.RUNNING) {
            TrackEvent.info("mode", "Closing window");
            App.getApp().close();
            waitForPlatform();
        }
    }

    @Override
    public ErrorHandler getErrorHandler() {
        var log = new LogErrorHandler();
        return new SyncErrorHandler(event -> {
            log.handle(event);
            ErrorHandlerComp.showAndWait(event);
        });
    }
}
