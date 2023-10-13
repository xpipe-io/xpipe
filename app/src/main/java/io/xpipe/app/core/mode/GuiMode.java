package io.xpipe.app.core.mode;

import io.xpipe.app.core.App;
import io.xpipe.app.core.AppGreetings;
import io.xpipe.app.core.AppMainWindow;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.update.CommercializationAlert;
import io.xpipe.app.update.UpdateChangelogAlert;
import io.xpipe.app.util.UnlockAlert;
import javafx.stage.Stage;

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

        TrackEvent.info("mode", "Waiting for window setup completion ...");
        PlatformThread.runLaterIfNeededBlocking(() -> {
            if (AppMainWindow.getInstance() == null) {
                try {
                    App.getApp().setupWindow();
                } catch (Throwable t) {
                    ErrorEvent.fromThrowable(t).terminal(true).handle();
                }
            }
            AppMainWindow.getInstance().show();
        });
        TrackEvent.info("mode", "Window setup complete");
    }

    @Override
    public void onSwitchFrom() {
        super.onSwitchFrom();
        PlatformThread.runLaterIfNeededBlocking(() -> {
            TrackEvent.info("mode", "Closing windows");
            Stage.getWindows().stream().toList().forEach(w -> w.hide());
        });
    }

}
