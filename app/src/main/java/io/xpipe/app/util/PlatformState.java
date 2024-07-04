package io.xpipe.app.util;

import io.xpipe.app.core.check.AppSystemFontCheck;
import io.xpipe.app.core.window.ModifiedStage;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.core.process.OsType;

import javafx.application.Platform;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.SystemUtils;

import java.awt.*;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

public enum PlatformState {
    NOT_INITIALIZED,
    RUNNING,
    EXITED;

    @Getter
    @Setter
    private static PlatformState current = PlatformState.NOT_INITIALIZED;

    @Getter
    private static Exception lastError;

    public static void teardown() {
        // This is bad and can get sometimes stuck
        //        PlatformThread.runLaterIfNeededBlocking(() -> {
        //            try {
        //                // Fix to preserve clipboard contents after shutdown
        //                var string = Clipboard.getSystemClipboard().getString();
        //                var s = new StringSelection(string);
        //                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(s, s);
        //            } catch (IllegalStateException ignored) {
        //            }
        //        });

        Platform.exit();
        setCurrent(PlatformState.EXITED);
    }

    public static void initPlatformOrThrow() throws Exception {
        initPlatformIfNeeded();
        if (lastError != null) {
            throw lastError;
        }
    }

    public static boolean initPlatformIfNeeded() {
        if (current == NOT_INITIALIZED) {
            var t = PlatformState.initPlatform().orElse(null);
            lastError = t instanceof Exception e ? e : t != null ? new Exception(t) : null;
        }

        return current == RUNNING;
    }

    private static Optional<Throwable> initPlatform() {
        if (current == EXITED) {
            return Optional.of(new IllegalStateException("Platform has already exited"));
        }

        if (current == RUNNING) {
            return Optional.empty();
        }

        try {
            // Weird fix to ensure that macOS quit operation works while in tray.
            // Maybe related to https://bugs.openjdk.org/browse/JDK-8318129 as it prints the same error if not called
            GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();

            // Catch more than just the headless exception in case the graphics environment initialization completely
            // fails
        } catch (HeadlessException h) {
            var msg = (OsType.getLocal().equals(OsType.LINUX)
                            ? "No X11 DISPLAY variable was set or no headful library support was found."
                            : "The application does not have desktop access, but this program performed an operation which requires it.")
                    + "\n\n"
                    + "Please note that XPipe is a desktop application that should be run on your local workstation."
                    + " It is able to provide the full functionality for all integrations via remote server connections, e.g. via SSH."
                    + " You don't have to install XPipe on any system like a server, a WSL distribution, a hypervisor, etc.,"
                    + " to have full access to that system, a shell connection to it is enough for XPipe to work from your local machine.";
            PlatformState.setCurrent(PlatformState.EXITED);
            return Optional.of(ErrorEvent.expected(new UnsupportedOperationException(msg)));
        } catch (Throwable t) {
            TrackEvent.warn(t.getMessage());
            PlatformState.setCurrent(PlatformState.EXITED);
            return Optional.of(t);
        }

        // Check if we have no fonts and set properties to load bundled ones
        AppSystemFontCheck.init();

        if (AppPrefs.get() != null) {
            var s = AppPrefs.get().uiScale().getValue();
            if (s != null) {
                var i = Math.min(300, Math.max(25, s));
                var value = i + "%";
                switch (OsType.getLocal()) {
                    case OsType.Linux linux -> {
                        System.setProperty("glass.gtk.uiScale", value);
                    }
                    case OsType.Windows windows -> {
                        System.setProperty("glass.win.uiScale", value);
                    }
                    default -> {}
                }
            }
        }

        if (SystemUtils.IS_OS_WINDOWS && ModifiedStage.mergeFrame()) {
            // This is primarily intended to fix Windows unified stage transparency issues (https://bugs.openjdk.org/browse/JDK-8329382)
            System.setProperty("prism.forceUploadingPainter", "true");
        }

        try {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.setImplicitExit(false);
            Platform.startup(() -> {
                latch.countDown();
            });
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
