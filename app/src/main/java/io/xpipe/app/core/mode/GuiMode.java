package io.xpipe.app.core.mode;

import io.xpipe.app.browser.file.BrowserLocalFileSystem;
import io.xpipe.app.browser.icon.BrowserIconManager;
import io.xpipe.app.comp.base.AppLayoutComp;
import io.xpipe.app.core.App;
import io.xpipe.app.core.AppGreetings;
import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.core.check.AppPtbCheck;
import io.xpipe.app.core.window.AppMainWindow;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.update.UpdateChangelogAlert;
import io.xpipe.app.util.NativeBridge;
import io.xpipe.app.util.PlatformThread;
import io.xpipe.app.util.ThreadHelper;

import javafx.stage.Stage;

public class GuiMode extends PlatformMode {

    @Override
    public String getId() {
        return "gui";
    }

    @Override
    public void onSwitchFrom() {
        PlatformThread.runLaterIfNeededBlocking(() -> {
            TrackEvent.info("Closing windows");
            Stage.getWindows().stream().toList().forEach(w -> {
                w.hide();
            });
        });
    }

    @Override
    public void onSwitchTo() throws Throwable {
        super.onSwitchTo();
        AppPtbCheck.check();
    }

    @Override
    public void finalTeardown() throws Throwable {
        BrowserLocalFileSystem.reset();
        super.finalTeardown();
    }
}
