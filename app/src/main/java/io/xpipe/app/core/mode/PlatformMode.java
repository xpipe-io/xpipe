package io.xpipe.app.core.mode;

import io.xpipe.app.comp.storage.collection.SourceCollectionViewState;
import io.xpipe.app.comp.storage.store.StoreViewState;
import io.xpipe.app.core.*;
import io.xpipe.app.update.UpdateAvailableAlert;
import io.xpipe.extension.event.TrackEvent;
import io.xpipe.extension.util.ThreadHelper;
import javafx.application.Application;
import javafx.application.Platform;

import java.awt.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public abstract class PlatformMode extends OperationMode {

    private static boolean stateInitialized;
    public static boolean HAS_GRAPHICS;
    public static boolean PLATFORM_LOADED;

    private static void initState() {
        if (stateInitialized) {
            return;
        }

        try {
            GraphicsDevice[] screenDevices =
                    GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
            HAS_GRAPHICS = screenDevices != null && screenDevices.length > 0;
        } catch (HeadlessException e) {
            TrackEvent.warn(e.getMessage());
            HAS_GRAPHICS = false;
        }

        try {
            Platform.startup(() -> {});
            PLATFORM_LOADED = true;
        } catch (Throwable t) {
            TrackEvent.warn(t.getMessage());
            PLATFORM_LOADED = false;
        }

        stateInitialized = true;
    }

    @Override
    public boolean isSupported() {
        initState();
        return HAS_GRAPHICS && PLATFORM_LOADED;
    }

    protected void platformSetup() {
        if (App.getApp() != null) {
            throw new AssertionError();
        }

        TrackEvent.info("mode", "Platform mode initial setup");
        AppI18n.init();
        AppFont.loadFonts();
        AppStyle.init();
        AppImages.init();
        TrackEvent.info("mode", "Finished essential component initialization before platform");

        TrackEvent.info("mode", "Launching application ...");
        ThreadHelper.create("app", false, () -> {
                    TrackEvent.info("mode", "Application thread started");
                    Application.launch(App.class);
                })
                .start();

        TrackEvent.info("mode", "Waiting for platform application startup ...");
        while (App.getApp() == null) {
            ThreadHelper.sleep(100);
        }

        UpdateAvailableAlert.showIfNeeded();

        SourceCollectionViewState.init();
        StoreViewState.init();
    }

    protected void waitForPlatform() {
        // The platform thread waits for the shutdown hook to finish in case SIGTERM is sent.
        // Therefore, we do not wait for the platform when being in a shutdown hook.
        if (App.isPlatformRunning() && !Platform.isFxApplicationThread() && !OperationMode.isInShutdownHook()) {
            TrackEvent.info("mode", "Waiting for platform thread ...");
            CountDownLatch latch = new CountDownLatch(1);
            Platform.runLater(latch::countDown);
            try {
                if (!latch.await(5, TimeUnit.SECONDS)) {
                    TrackEvent.info("mode", "Platform wait timed out");
                }
            } catch (InterruptedException ignored) {
            }
            TrackEvent.info("mode", "Synced with platform thread");
        } else {
            TrackEvent.info("mode", "Not waiting for platform thread");
        }
    }

    @Override
    public void initialSetup() throws Throwable {
        BACKGROUND.initialSetup();
        onSwitchTo();
    }

    @Override
    public void finalTeardown() throws Throwable {
        TrackEvent.info("mode", "Shutting down platform components");
        onSwitchFrom();
        Platform.exit();
        TrackEvent.info("mode", "Platform shutdown finished");
        BACKGROUND.finalTeardown();
    }
}
