package io.xpipe.app.core.mode;

import io.xpipe.app.core.AppTray;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.issue.*;
import io.xpipe.core.process.OsType;

import java.awt.*;

public class TrayMode extends PlatformMode {

    @Override
    public boolean isSupported() {
        return OsType.getLocal().equals(OsType.WINDOWS)
                && super.isSupported()
                && Desktop.isDesktopSupported()
                && SystemTray.isSupported();
    }

    @Override
    public void onSwitchTo() throws Throwable {
        super.onSwitchTo();
        PlatformThread.runLaterIfNeededBlocking(() -> {
            if (AppTray.get() == null) {
                TrackEvent.info("Initializing tray");
                AppTray.init();
            }

            AppTray.get().show();
            TrackEvent.info("Finished tray initialization");
        });
    }

    @Override
    public String getId() {
        return "tray";
    }

    @Override
    public void onSwitchFrom() {
        if (AppTray.get() != null) {
            TrackEvent.info("Closing tray");
            PlatformThread.runLaterIfNeededBlocking(() -> AppTray.get().hide());
        }
    }

    @Override
    public ErrorHandler getErrorHandler() {
        var log = new LogErrorHandler();
        return new SyncErrorHandler(event -> {
            // Check if tray initialization is finished
            if (AppTray.get() != null) {
                AppTray.get().getErrorHandler().handle(event);
            }
            log.handle(event);
            ErrorAction.ignore().handle(event);
        });
    }
}
