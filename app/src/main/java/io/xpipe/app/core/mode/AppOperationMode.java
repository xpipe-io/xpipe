package io.xpipe.app.core.mode;

import io.xpipe.app.beacon.AppBeaconServer;
import io.xpipe.app.core.*;
import io.xpipe.app.core.check.AppDebugModeCheck;
import io.xpipe.app.core.window.AppMainWindow;
import io.xpipe.app.issue.*;
import io.xpipe.app.platform.PlatformInit;
import io.xpipe.app.platform.PlatformState;
import io.xpipe.app.platform.PlatformThreadWatcher;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.prefs.CloseBehaviour;
import io.xpipe.app.process.LocalShell;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.update.AppDistributionType;
import io.xpipe.app.util.*;
import io.xpipe.core.FailableRunnable;
import io.xpipe.core.XPipeDaemonMode;

import javafx.application.Platform;

import lombok.Getter;
import lombok.SneakyThrows;

import java.time.Duration;
import java.util.List;

public abstract class AppOperationMode {

    public static final AppOperationMode BACKGROUND = new AppBaseMode();
    public static final AppOperationMode TRAY = new AppTrayMode();
    public static final AppOperationMode GUI = new AppGuiMode();
    private static final List<AppOperationMode> ALL = List.of(BACKGROUND, TRAY, GUI);
    private static final Object HALT_LOCK = new Object();

    @Getter
    private static boolean inStartup;

    @Getter
    private static boolean inShutdown;

    @Getter
    private static boolean inShutdownHook;

    private static AppOperationMode CURRENT = null;

    public static AppOperationMode map(XPipeDaemonMode mode) {
        return switch (mode) {
            case BACKGROUND -> BACKGROUND;
            case TRAY -> TRAY;
            case GUI -> GUI;
        };
    }

    public static void externalShutdown() {
        // If we used System.exit(), we don't want to do this
        if (AppOperationMode.isInShutdown()) {
            return;
        }

        inShutdownHook = true;
        TrackEvent.info("Received SIGTERM externally");
        AppOperationMode.shutdown(false);
    }

    private static void setup(String[] args) {
        try {
            // Only for handling SIGTERM
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                externalShutdown();
            }));

            // Handle uncaught exceptions
            Thread.setDefaultUncaughtExceptionHandler((thread, ex) -> {
                // It seems like a few exceptions are thrown in the quantum renderer
                // when in shutdown. We can ignore these
                if (AppOperationMode.isInShutdown()
                        && Platform.isFxApplicationThread()
                        && ex instanceof NullPointerException) {
                    return;
                }

                // There are some accessibility exceptions on macOS, nothing we can do about that
                if (Platform.isFxApplicationThread()
                        && ex instanceof NullPointerException
                        && ex.getMessage() != null
                        && ex.getMessage().contains("Accessible")) {
                    ErrorEventFactory.fromThrowable(ex)
                            .expected()
                            .description(
                                    "An error occurred with the Accessibility implementation. A screen reader might not be supported right now")
                            .build()
                            .handle();
                    return;
                }

                // Handle any startup uncaught errors
                if (AppOperationMode.isInStartup() && thread.threadId() == 1) {
                    ex.printStackTrace();
                    AppOperationMode.halt(1);
                }

                if (ex instanceof OutOfMemoryError) {
                    ex.printStackTrace();
                    AppOperationMode.halt(1);
                }

                ErrorEventFactory.fromThrowable(ex).unhandled(true).build().handle();
            });

            TrackEvent.info("Initial setup");
            AppMainWindow.loadingText("initializingApp");
            GlobalTimer.init();
            AppProperties.init(args);
            PlatformThreadWatcher.init();
            AppLogs.init();
            AppDebugModeCheck.printIfNeeded();
            AppProperties.get().logArguments();
            AppDistributionType.init();
            AppExtensionManager.init();
            AppI18n.init();
            AppPrefs.initLocal();
            AppBeaconServer.setupPort();
            AppInstance.init();
            // Initialize early to load in parallel
            PlatformInit.init(false);
            ThreadHelper.runAsync(() -> {
                PlatformInit.init(true);
                AppMainWindow.init(AppOperationMode.getStartupMode() == XPipeDaemonMode.GUI);
            });
            TrackEvent.info("Finished initial setup");
        } catch (Throwable ex) {
            ErrorEventFactory.fromThrowable(ex).term().handle();
        }
    }

    public static XPipeDaemonMode getStartupMode() {
        var event = TrackEvent.withInfo("Startup mode determined");
        if (AppMainWindow.get() != null && AppMainWindow.get().getStage().isShowing()) {
            event.tag("mode", "gui").tag("reason", "windowShowing").handle();
            return XPipeDaemonMode.GUI;
        }

        var prop = AppProperties.get().getExplicitMode();
        if (prop != null) {
            event.tag("mode", prop.getDisplayName())
                    .tag("reason", "modePropertyPassed")
                    .handle();
            return prop;
        }

        if (AppPrefs.get() != null) {
            var pref = AppPrefs.get().startupBehaviour().getValue().getMode();
            event.tag("mode", pref.getDisplayName())
                    .tag("reason", "prefSetting")
                    .handle();
            return pref;
        }

        event.tag("mode", "gui").tag("reason", "fallback").handle();
        return XPipeDaemonMode.GUI;
    }

    public static void init(String[] args) {
        inStartup = true;
        setup(args);

        try {
            if (AppProperties.get().isAotTrainMode()) {
                AppOperationMode.switchToSyncOrThrow(BACKGROUND);
                inStartup = false;
                AppAotTrain.runTrainingMode();
                AppOperationMode.shutdown(false);
                return;
            }

            var startupMode = getStartupMode();
            switchToSyncOrThrow(map(startupMode));
            // If it doesn't find time, the JVM will not gc the startup workload
            System.gc();
            inStartup = false;
            AppOpenArguments.init();
            ThreadHelper.runAsync(() -> {
                DataStorage.get().generateCaches();
            });
        } catch (Throwable ex) {
            ErrorEventFactory.fromThrowable(ex).term().handle();
        }
    }

    public static void switchToAsync(AppOperationMode newMode) {
        ThreadHelper.createPlatformThread("mode switcher", false, () -> {
                    switchToSyncIfPossible(newMode);
                })
                .start();
    }

    public static void switchToSyncOrThrow(AppOperationMode newMode) throws Throwable {
        TrackEvent.info("Attempting to switch mode to " + newMode.getId());

        if (!newMode.isSupported()) {
            throw PlatformState.getLastError() != null
                    ? PlatformState.getLastError()
                    : new IllegalStateException("Unsupported operation mode: " + newMode.getId());
        }

        set(newMode);
    }

    public static boolean switchToSyncIfPossible(AppOperationMode newMode) {
        TrackEvent.info("Attempting to switch mode to " + newMode.getId());

        if (newMode.equals(TRAY) && !TRAY.isSupported()) {
            TrackEvent.info("Tray is not available, using base instead");
            set(BACKGROUND);
            return false;
        }

        if (newMode.equals(GUI) && !GUI.isSupported()) {
            TrackEvent.info("Gui is not available, using base instead");
            set(BACKGROUND);
            return false;
        }

        set(newMode);
        return true;
    }

    public static void close() {
        set(null);
    }

    public static List<AppOperationMode> getAll() {
        return ALL;
    }

    public static void executeAfterShutdown(FailableRunnable<Exception> r) {
        Runnable exec = () -> {
            if (inShutdown) {
                return;
            }

            try {
                if (!isInStartup()) {
                    inShutdown = true;
                    if (CURRENT != null) {
                        CURRENT.finalTeardown();
                    }
                    CURRENT = null;
                }

                // Restart local shell
                LocalShell.init();
                r.run();
            } catch (Throwable ex) {
                ErrorEventFactory.fromThrowable(ex).handle();
                AppOperationMode.halt(1);
            }

            // In case we perform any operations such as opening a terminal
            // give it some time to open while this process is still alive
            // Otherwise it might quit because the parent process is dead already
            ThreadHelper.sleep(100);
            AppOperationMode.halt(0);
        };

        // Creates separate non daemon thread to force execution after shutdown even if current thread is a daemon
        var t = new Thread(exec);
        t.setDaemon(false);
        t.start();
    }

    public static void halt(int code) {
        synchronized (HALT_LOCK) {
            TrackEvent.info("Halting now!");
            AppLogs.teardown();
            Runtime.getRuntime().halt(code);
        }
    }

    public static void onWindowClose() {
        CloseBehaviour action;
        if (AppPrefs.get() != null && !isInStartup() && !isInShutdown()) {
            action = AppPrefs.get().closeBehaviour().getValue();
        } else {
            action = CloseBehaviour.QUIT;
        }
        ThreadHelper.runAsync(() -> {
            action.run();
        });
    }

    @SneakyThrows
    public static void shutdown(boolean hasError) {
        if (isInStartup()) {
            TrackEvent.info("Received shutdown request while in startup. Halting ...");
            AppOperationMode.halt(1);
        }

        TrackEvent.info("Starting shutdown ...");

        synchronized (AppOperationMode.class) {
            if (inShutdown) {
                return;
            }

            inShutdown = true;
        }

        // Keep a non-daemon thread running
        var thread = ThreadHelper.createPlatformThread("shutdown", false, () -> {
            try {
                if (CURRENT != null) {
                    CURRENT.finalTeardown();
                }
                CURRENT = null;
            } catch (Throwable t) {
                ErrorEventFactory.fromThrowable(t).term().handle();
                AppOperationMode.halt(1);
            }

            AppOperationMode.halt(hasError ? 1 : 0);
        });
        thread.start();

        // Use a timer to always exit after some time in case we get stuck
        var limit = !hasError && !AppProperties.get().isDevelopmentEnvironment() ? 25000 : Integer.MAX_VALUE;
        var exited = thread.join(Duration.ofMillis(limit));
        if (!exited) {
            TrackEvent.info("Shutdown took too long. Halting ...");
            AppOperationMode.halt(1);
        }
    }

    private static synchronized void set(AppOperationMode newMode) {
        if (inShutdown) {
            return;
        }

        if (CURRENT == null && newMode == null) {
            return;
        }

        if (CURRENT != null && CURRENT.equals(newMode)) {
            return;
        }

        try {
            if (newMode == null) {
                shutdown(false);
                return;
            }

            if (CURRENT != null && CURRENT != BACKGROUND) {
                CURRENT.onSwitchFrom();
            }

            BACKGROUND.onSwitchTo();
            if (newMode != GUI
                    && AppMainWindow.get() != null
                    && AppMainWindow.get().getStage().isShowing()) {
                GUI.onSwitchTo();
                newMode = GUI;
            } else {
                newMode.onSwitchTo();
            }
            CURRENT = newMode;
        } catch (Throwable ex) {
            ErrorEventFactory.fromThrowable(ex).terminal(true).build().handle();
        }
    }

    public static AppOperationMode get() {
        return CURRENT;
    }

    public abstract boolean isSupported();

    public abstract String getId();

    public abstract void onSwitchTo() throws Throwable;

    public abstract void onSwitchFrom();

    public abstract void finalTeardown() throws Throwable;

    public ErrorHandler getErrorHandler() {
        return new SyncErrorHandler(new GuiErrorHandler());
    }
}
