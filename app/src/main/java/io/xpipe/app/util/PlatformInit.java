package io.xpipe.app.util;

import io.xpipe.app.core.*;
import io.xpipe.app.core.check.AppGpuCheck;
import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.core.window.AppMainWindow;
import io.xpipe.app.core.window.ModifiedStage;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.TrackEvent;

import io.xpipe.core.util.XPipeDaemonMode;
import javafx.application.Application;
import lombok.SneakyThrows;

import java.util.concurrent.CountDownLatch;

public class PlatformInit {

    private static final CountDownLatch latch = new CountDownLatch(2);

    @SneakyThrows
    public static synchronized void init(boolean wait) {
        // Already finished
        if (latch.getCount() == 0) {
            return;
        }

        // Another thread is loading
        if (latch.getCount() == 1) {
            if (wait) {
                latch.await();
            }
            return;
        }

        if (latch.getCount() == 2) {
            latch.countDown();
        }

        ThreadHelper.runAsync(() -> {
            initSync();
        });
        if (wait) {
            latch.await();
        }
    }

    private static synchronized void initSync() {
        try {
            TrackEvent.info("Platform init started");
            ModifiedStage.init();
            PlatformState.initPlatformOrThrow();
            AppGpuCheck.check();
            AppFont.init();
            AppStyle.init();
            AppTheme.init();
            AppI18n.init();
            AppDesktopIntegration.init();

            // Must not be called on platform thread
            ThreadHelper.runAsync(() -> {
                Application.launch(App.class);
            });
            while (App.getApp() == null) {
                ThreadHelper.sleep(100);
            }
            NativeBridge.init();
            PlatformThread.runLaterIfNeededBlocking(() -> {
                AppMainWindow.initEmpty(OperationMode.getStartupMode() == XPipeDaemonMode.GUI);
            });
            TrackEvent.info("Platform init finished");
            latch.countDown();
        } catch (Throwable t) {
            ErrorEvent.fromThrowable(t).term().handle();
            latch.countDown();
        }
    }
}
