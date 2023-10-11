package io.xpipe.app.core.mode;

import io.xpipe.app.core.App;
import io.xpipe.app.core.AppGreetings;
import io.xpipe.app.core.AppMainWindow;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.issue.*;
import io.xpipe.app.update.CommercializationAlert;
import io.xpipe.app.update.UpdateChangelogAlert;
import io.xpipe.app.util.UnlockAlert;
import javafx.application.Platform;

import java.util.concurrent.CountDownLatch;

public class GuiMode extends PlatformMode {

    @Override
    public String getId() {
        return "gui";
    }

    @Override
    public void onSwitchTo() throws Throwable {
        super.onSwitchTo();

        UnlockAlert.showIfNeeded();
        UpdateChangelogAlert.showIfNeeded();
        CommercializationAlert.showIfNeeded();
        AppGreetings.showIfNeeded();

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            if (AppMainWindow.getInstance() == null) {
                try {
                    App.getApp().setupWindow();
                    AppMainWindow.getInstance().show();
                    latch.countDown();
                } catch (Throwable t) {
                    ErrorEvent.fromThrowable(t).terminal(true).handle();
                }
            } else {
                latch.countDown();
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
        super.onSwitchFrom();
        PlatformThread.runLaterIfNeededBlocking(() -> {
            TrackEvent.info("mode", "Closing window");
            App.getApp().close();
        });
    }

    @Override
    public ErrorHandler getErrorHandler() {
        return new SyncErrorHandler(new GuiErrorHandler());
    }
}
