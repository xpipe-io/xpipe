package io.xpipe.app.util;

import io.xpipe.app.core.*;
import io.xpipe.app.core.check.AppGpuCheck;
import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.core.window.AppMainWindow;
import io.xpipe.app.core.window.ModifiedStage;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.TrackEvent;

import io.xpipe.core.process.OsType;
import io.xpipe.core.util.XPipeDaemonMode;
import javafx.application.Application;
import lombok.SneakyThrows;

import java.util.concurrent.CountDownLatch;

public class PlatformInit {

    private static final CountDownLatch latch = new CountDownLatch(2);
    private static Thread loadingThread;
    private static Throwable error;

    @SneakyThrows
    public static synchronized void init(boolean wait) {
        // Already finished
        if (latch.getCount() == 0) {
            if (error != null) {
                throw error;
            }

            return;
        }

        // Another thread is loading
        if (latch.getCount() == 1) {
            if (Thread.currentThread() == loadingThread) {
                return;
            }

            if (wait) {
                latch.await();
            }

            if (error != null) {
                throw error;
            }

            return;
        }

        if (latch.getCount() == 2) {
            latch.countDown();
        }

        ThreadHelper.runAsync(() -> {
            loadingThread = Thread.currentThread();
            initSync();
            loadingThread = null;
        });
        if (wait) {
            if (error != null) {
                throw error;
            }
            latch.await();
        }
    }

    private static void initSync() {
        if (AppProperties.get().isAotTrainMode() && OsType.getLocal() == OsType.LINUX) {
            latch.countDown();
            return;
        }

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
                ThreadHelper.sleep(10);
            }
            NativeBridge.init();
            TrackEvent.info("Platform init finished");
            latch.countDown();
        } catch (Throwable t) {
            error = t;
            latch.countDown();
        }
    }
}
