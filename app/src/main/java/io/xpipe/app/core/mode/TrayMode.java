package io.xpipe.app.core.mode;

import com.dustinredmond.fxtrayicon.FXTrayIcon;
import io.xpipe.app.core.App;
import io.xpipe.app.core.AppTray;
import io.xpipe.app.issue.ErrorHandler;
import io.xpipe.app.issue.TrackEvent;

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
    public void onSwitchTo() {
        if (!App.isPlatformRunning()) {
            super.platformSetup();
        }

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
        if (AppTray.get() != null) {
            return AppTray.get().getErrorHandler();
        } else {
            return BACKGROUND.getErrorHandler();
        }
    }
}
