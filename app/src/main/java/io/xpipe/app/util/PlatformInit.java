package io.xpipe.app.util;

import io.xpipe.app.core.AppDesktopIntegration;
import io.xpipe.app.core.AppStyle;
import io.xpipe.app.core.AppTheme;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.TrackEvent;

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
            PlatformState.initPlatformOrThrow();
            AppStyle.init();
            AppTheme.init();
            AppDesktopIntegration.init();
            TrackEvent.info("Platform init finished");
            latch.countDown();
        } catch (Throwable t) {
            ErrorEvent.fromThrowable(t).term().handle();
            latch.countDown();
        }
    }
}
