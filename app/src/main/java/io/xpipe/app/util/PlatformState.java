package io.xpipe.app.util;

import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.issue.TrackEvent;
import javafx.application.Platform;
import javafx.scene.input.Clipboard;
import lombok.Getter;
import lombok.Setter;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

public enum PlatformState {
    NOT_INITIALIZED,
    RUNNING,
    EXITED;

    @Getter
    @Setter
    private static PlatformState current = PlatformState.NOT_INITIALIZED;

    public static boolean HAS_GRAPHICS;
    public static boolean PLATFORM_LOADED;

    public static void teardown() {
        PlatformThread.runLaterIfNeededBlocking(() -> {
            // Fix to preserve clipboard contents after shutdown
            var string = Clipboard.getSystemClipboard().getString();
            var s = new StringSelection(string);
            try {
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(s, s);
            } catch (IllegalStateException ignored) {
            }
        });

        Platform.exit();
        setCurrent(PlatformState.EXITED);
    }

    public static void initPlatformOrThrow() throws Throwable {
        var r = PlatformState.initPlatform();
        if (r.isPresent()) {
            throw r.get();
        }
    }

    public static Optional<Throwable> initPlatform() {
        if (current == EXITED) {
            return Optional.of(new IllegalStateException("Platform has already exited"));
        }

        if (current == RUNNING) {
            return Optional.empty();
        }

        try {
            GraphicsDevice[] screenDevices =
                    GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
            HAS_GRAPHICS = screenDevices != null && screenDevices.length > 0;
        } catch (HeadlessException e) {
            TrackEvent.warn(e.getMessage());
            HAS_GRAPHICS = false;
            return Optional.of(e);
        }

        try {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.setImplicitExit(false);
            Platform.startup(latch::countDown);
            try {
                latch.await();
                PLATFORM_LOADED = true;
                PlatformState.setCurrent(PlatformState.RUNNING);
                return Optional.empty();
            } catch (InterruptedException e) {
                return Optional.of(e);
            }
        } catch (Throwable t) {
            // Check if we already exited
            if ("Platform.exit has been called".equals(t.getMessage())) {
                PLATFORM_LOADED = true;
                PlatformState.setCurrent(PlatformState.EXITED);
                return Optional.of(t);
            } else if ("Toolkit already initialized".equals(t.getMessage())) {
                PLATFORM_LOADED = true;
                PlatformState.setCurrent(PlatformState.RUNNING);
                return Optional.empty();
            } else {
                // Platform initialization has failed in this case
                PLATFORM_LOADED = false;
                PlatformState.setCurrent(PlatformState.EXITED);
                TrackEvent.error(t.getMessage());
                return Optional.of(t);
            }
        }
    }
}
