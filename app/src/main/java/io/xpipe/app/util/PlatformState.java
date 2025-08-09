package io.xpipe.app.util;

import io.xpipe.app.core.check.AppSystemFontCheck;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.core.OsType;

import javafx.application.Platform;
import javafx.scene.text.Font;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.apache.commons.lang3.SystemUtils;

import java.awt.*;
import java.util.concurrent.CountDownLatch;

public enum PlatformState {
    NOT_INITIALIZED,
    RUNNING,
    EXITED;

    @Getter
    @Setter
    private static PlatformState current = PlatformState.NOT_INITIALIZED;

    private static Throwable lastError;
    private static boolean expectedError;

    public static Throwable getLastError() {
        if (expectedError) {
            ErrorEventFactory.expected(lastError);
        }
        return lastError;
    }

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
        setCurrent(PlatformState.EXITED);

        // Give other threads, e.g. windows shutdown hook time to properly signal exit state
        ThreadHelper.sleep(100);

        Platform.exit();
    }

    public static void initPlatformOrThrow() throws Throwable {
        if (current == NOT_INITIALIZED) {
            PlatformState.initPlatform();
        }
        if (lastError != null) {
            throw getLastError();
        }
    }

    private static String getErrorMessage(String message) {
        var header = message != null ? message + "\n\n" : "Failed to load graphics support\n\n";
        var msg = header
                + "Please note that XPipe is a desktop application that should be run on your local workstation."
                + " It is able to provide the full functionality for all integrations via remote server connections, e.g. via SSH."
                + " You don't have to install XPipe on any system like a server, a WSL distribution, a hypervisor, etc.,"
                + " to have full access to that system, a shell connection to it is enough for XPipe to work from your local machine.";
        return msg;
    }

    private static void initPlatform() {
        if (current == EXITED) {
            lastError = new IllegalStateException("Platform has already exited");
            return;
        }

        if (current == RUNNING) {
            return;
        }

        try {
            // Weird fix to ensure that macOS quit operation works while in tray.
            // Maybe related to https://bugs.openjdk.org/browse/JDK-8318129 as it prints the same error if not called
            GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();

            // Catch more than just the headless exception in case the graphics environment initialization completely
            // fails
        } catch (HeadlessException h) {
            var msg = getErrorMessage(h.getMessage());
            PlatformState.setCurrent(PlatformState.EXITED);
            expectedError = true;
            lastError = new UnsupportedOperationException(msg, h);
            return;
        } catch (Throwable t) {
            PlatformState.setCurrent(PlatformState.EXITED);
            lastError = t;
            return;
        }

        // Check if we have no fonts and set properties to load bundled ones
        AppSystemFontCheck.init();

        if (AppPrefs.get() != null) {
            var s = AppPrefs.get().uiScale().getValue();
            if (s != null) {
                var i = Math.min(300, Math.max(25, s));
                var value = i + "%";
                switch (OsType.getLocal()) {
                    case OsType.Linux ignored -> {
                        System.setProperty("glass.gtk.uiScale", value);
                    }
                    case OsType.Windows ignored -> {
                        System.setProperty("glass.win.uiScale", value);
                    }
                    default -> {}
                }
            }
        }

        if (SystemUtils.IS_OS_WINDOWS) {
            // This is primarily intended to fix Windows unified stage transparency issues
            // (https://bugs.openjdk.org/browse/JDK-8329382)
            // But apparently it can also occur without a custom stage on Windows
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
            } catch (InterruptedException e) {
                lastError = e;
                return;
            }
        } catch (Throwable t) {
            // Check if we already exited
            if ("Platform.exit has been called".equals(t.getMessage())) {
                PlatformState.setCurrent(PlatformState.EXITED);
                lastError = t;
                return;
            } else if ("Toolkit already initialized".equals(t.getMessage())) {
                PlatformState.setCurrent(PlatformState.RUNNING);
            } else {
                // Platform initialization has failed in this case
                var msg = getErrorMessage(t.getMessage());
                var ex = new UnsupportedOperationException(msg, t);
                PlatformState.setCurrent(PlatformState.EXITED);
                lastError = ex;
                return;
            }
        }

        // We use our own shutdown hook
        disableToolkitShutdownHook();

        try {
            // This can fail if the found system fonts can somehow not be loaded
            Font.getDefault();
        } catch (Throwable e) {
            var ex = new IllegalStateException("Unable to load fonts. Do you have a valid font package installed?", e);
            lastError = ex;
            PlatformState.setCurrent(PlatformState.EXITED);
            return;
        }
    }

    @SneakyThrows
    private static void disableToolkitShutdownHook() {
        var tkClass = Class.forName(
                ModuleLayer.boot().findModule("javafx.graphics").orElseThrow(), "com.sun.javafx.tk.Toolkit");
        var getToolkitMethod = tkClass.getDeclaredMethod("getToolkit");
        getToolkitMethod.setAccessible(true);
        var tk = getToolkitMethod.invoke(null);
        var shutdownHookField = tk.getClass().getDeclaredField("shutdownHook");
        shutdownHookField.setAccessible(true);
        var thread = (Thread) shutdownHookField.get(tk);
        Runtime.getRuntime().removeShutdownHook(thread);
    }
}
