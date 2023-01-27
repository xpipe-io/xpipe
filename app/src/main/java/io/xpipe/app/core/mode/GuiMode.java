package io.xpipe.app.core.mode;

import io.xpipe.app.grid.UpdateChangelogAlert;
import io.xpipe.app.core.App;
import io.xpipe.app.core.AppGreetings;
import io.xpipe.app.issue.ErrorHandler;
import io.xpipe.app.issue.ErrorHandlerComp;
import io.xpipe.extension.event.ErrorEvent;
import io.xpipe.extension.event.TrackEvent;
import javafx.application.Platform;

import java.util.concurrent.CountDownLatch;

public class GuiMode extends PlatformMode {

    @Override
    public String getId() {
        return "gui";
    }

    @Override
    public void onSwitchTo() {
        if (!App.isPlatformRunning()) {
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
        if (App.isPlatformRunning()) {
            TrackEvent.info("mode", "Closing window");
            App.getApp().close();
            waitForPlatform();
        }
    }

    @Override
    public ErrorHandler getErrorHandler() {
        return event -> {
            BACKGROUND.getErrorHandler().handle(event);
            if (App.isPlatformRunning() && !event.isOmitted()) {
                ErrorHandlerComp.showAndWait(event);
            }
        };
    }
}
