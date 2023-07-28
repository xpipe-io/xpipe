package io.xpipe.app.core.mode;

import com.dustinredmond.fxtrayicon.FXTrayIcon;
import io.xpipe.app.core.AppTray;
import io.xpipe.app.issue.*;

public class TrayMode extends PlatformMode {

    @Override
    public boolean isSupported() {
        return super.isSupported() && FXTrayIcon.isSupported();
    }

    @Override
    public String getId() {
        return "tray";
    }

    @Override
    public void onSwitchTo() throws Throwable {
        super.platformSetup();

        if (AppTray.get() == null) {
            TrackEvent.info("mode", "Initializing tray");
            AppTray.init();
        }

        AppTray.get().show();
        waitForPlatform();
        TrackEvent.info("mode", "Finished tray initialization");
    }

    @Override
    public void onSwitchFrom() {
        if (AppTray.get() != null) {
            TrackEvent.info("mode", "Closing tray");
            AppTray.get().hide();
            waitForPlatform();
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
