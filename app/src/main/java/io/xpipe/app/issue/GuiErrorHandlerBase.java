package io.xpipe.app.issue;

import io.xpipe.app.core.*;
import io.xpipe.app.util.PlatformState;
import javafx.application.Platform;

import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

public class GuiErrorHandlerBase {

    protected boolean startupGui(Consumer<Throwable> onFail) {
        if (PlatformState.getCurrent() == PlatformState.EXITED) {
            return false;
        }

        if (PlatformState.getCurrent() == PlatformState.NOT_INITIALIZED) {
            try {
                CountDownLatch latch = new CountDownLatch(1);
                Platform.setImplicitExit(false);
                Platform.startup(latch::countDown);
                try {
                    latch.await();
                    PlatformState.setCurrent(PlatformState.RUNNING);
                } catch (InterruptedException ignored) {
                }
            } catch (Throwable r) {
                // Check if we already exited
                if ("Platform.exit has been called".equals(r.getMessage())) {
                    PlatformState.setCurrent(PlatformState.EXITED);
                    return false;
                }

                if ("Toolkit already initialized".equals(r.getMessage())) {
                    PlatformState.setCurrent(PlatformState.RUNNING);
                } else {
                    // Platform initialization has failed in this case
                    onFail.accept(r);
                    return false;
                }
            }
        }

        try {
            AppProperties.init();
            AppState.init();
            AppExtensionManager.init(false);
            AppI18n.init();
            AppStyle.init();
            AppTheme.init();
        } catch (Throwable r) {
            onFail.accept(r);
            return false;
        }

        return true;
    }
}
