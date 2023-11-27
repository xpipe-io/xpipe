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
            CountDownLatch latch = new CountDownLatch(1);
            Platform.setImplicitExit(false);
            Platform.startup(latch::countDown);
            try {
                latch.await();
                PlatformState.setCurrent(PlatformState.RUNNING);
                return Optional.empty();
            } catch (InterruptedException e) {
                return Optional.of(e);
            }
        } catch (Throwable t) {
            // Check if we already exited
            if ("Platform.exit has been called".equals(t.getMessage())) {
                PlatformState.setCurrent(PlatformState.EXITED);
                return Optional.of(t);
            } else if ("Toolkit already initialized".equals(t.getMessage())) {
                PlatformState.setCurrent(PlatformState.RUNNING);
                return Optional.empty();
            } else {
                // Platform initialization has failed in this case
                PlatformState.setCurrent(PlatformState.EXITED);
                TrackEvent.error(t.getMessage());
                return Optional.of(t);
            }
        }
    }
}
