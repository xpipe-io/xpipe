package io.xpipe.app.core.mode;

import io.xpipe.app.core.App;
import io.xpipe.app.core.AppChecks;
import io.xpipe.app.core.AppLogs;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.ErrorHandler;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.launcher.LauncherCommand;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.app.util.XPipeSession;
import io.xpipe.core.util.XPipeDaemonMode;
import org.apache.commons.lang3.function.FailableRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public abstract class OperationMode {

    public static final String MODE_PROP = "io.xpipe.app.mode";
    public static final OperationMode BACKGROUND = new BaseMode();
    public static final OperationMode TRAY = new TrayMode();
    public static final OperationMode GUI = new GuiMode();
    private static final Pattern PROPERTY_PATTERN = Pattern.compile("^-[DP](.+)=(.+)$");
    private static final List<OperationMode> ALL = List.of(BACKGROUND, TRAY, GUI);
    private static boolean inStartup;
    private static boolean inShutdown;
    private static boolean inShutdownHook;
    private static OperationMode CURRENT = null;

    public static OperationMode map(XPipeDaemonMode mode) {
        return switch (mode) {
            case BACKGROUND -> BACKGROUND;
            case TRAY -> TRAY;
            case GUI -> GUI;
        };
    }

    public static XPipeDaemonMode map(OperationMode mode) {
        if (mode == BACKGROUND) {
            return XPipeDaemonMode.BACKGROUND;
        }

        if (mode == TRAY) {
            return XPipeDaemonMode.TRAY;
        }

        if (mode == GUI) {
            return XPipeDaemonMode.GUI;
        }

        return null;
    }

    private static String[] parseProperties(String[] args) {
        List<String> newArgs = new ArrayList<>();
        for (var a : args) {
            var m = PROPERTY_PATTERN.matcher(a);
            if (m.matches()) {
                var k = m.group(1);
                var v = m.group(2);
                System.setProperty(k, v);
            } else {
                newArgs.add(a);
            }
        }
        return newArgs.toArray(String[]::new);
    }

    private static void setup(String[] args) {
        try {
            // Only for handling SIGTERM
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                OperationMode.shutdown(true, false);
            }));

            // Handle uncaught exceptions
            Thread.setDefaultUncaughtExceptionHandler((thread, ex) -> {
                ErrorEvent.fromThrowable(ex).build().handle();
            });

            //            if (true) {
            //                throw new OutOfMemoryError();
            //            }

            TrackEvent.info("mode", "Initial setup");
            AppProperties.init();
            XPipeSession.init(AppProperties.get().getBuildUuid());
            AppChecks.checkDirectoryPermissions();
            AppLogs.init();
            AppProperties.logArguments(args);
            AppProperties.logSystemProperties();
            AppProperties.logPassedProperties();
            TrackEvent.info("mode", "Finished initial setup");
        } catch (Throwable ex) {
            ErrorEvent.fromThrowable(ex).term().handle();
        }
    }

    public static void init(String[] args) {
        inStartup = true;
        var usedArgs = parseProperties(args);
        setup(args);
        LauncherCommand.runLauncher(usedArgs);
        inStartup = false;
    }

    public static boolean isInStartup() {
        return inStartup;
    }

    public static boolean isInShutdown() {
        return inShutdown;
    }

    public static void switchToAsync(OperationMode newMode) {
        ThreadHelper.create("mode switcher", false, () -> switchTo(newMode)).start();
    }

    public static void switchTo(OperationMode newMode) {
        TrackEvent.info("Attempting to switch mode to " + newMode.getId());

        if (newMode.equals(TRAY) && !TRAY.isSupported()) {
            TrackEvent.info("Tray is not available, using base instead");
            set(BACKGROUND);
            return;
        }

        if (newMode.equals(GUI) && !GUI.isSupported()) {
            TrackEvent.info("Gui is not available, using base instead");
            set(BACKGROUND);
            return;
        }

        set(newMode);
    }

    public static void switchUp(OperationMode newMode) {
        if (newMode == BACKGROUND) {
            return;
        }

        TrackEvent.info("Attempting to switch mode up to " + newMode.getId());

        if (newMode.equals(TRAY) && TRAY.isSupported() && OperationMode.get() == BACKGROUND) {
            set(TRAY);
            return;
        }

        if (newMode.equals(GUI) && GUI.isSupported()) {
            set(GUI);
            App.getApp().focus();
        }
    }

    public static void close() {
        set(null);
    }

    public static List<OperationMode> getAll() {
        return ALL;
    }

    public static void executeAfterShutdown(FailableRunnable<Exception> r) {
        if (inShutdown) {
            return;
        }

        inShutdown = true;
        inShutdownHook = false;
        try {
            if (CURRENT != null) {
                CURRENT.finalTeardown();
            }
            CURRENT = null;
            r.run();
        } catch (Throwable t) {
            ErrorEvent.fromThrowable(t).build().handle();
            OperationMode.halt(1);
        }

        OperationMode.halt(0);
    }

    public static void halt(int code) {
        AppLogs.teardown();
        Runtime.getRuntime().halt(code);
    }

    public static void shutdown(boolean inShutdownHook, boolean hasError) {
        if (inShutdown) {
            return;
        }

        inShutdown = true;
        OperationMode.inShutdownHook = inShutdownHook;
        try {
            if (CURRENT != null) {
                CURRENT.finalTeardown();
            }
            CURRENT = null;
        } catch (Throwable t) {
            ErrorEvent.fromThrowable(t).term().handle();
            OperationMode.halt(1);
        }

        OperationMode.halt(hasError ? 1 : 0);
    }

    public static synchronized void reload() {
        ThreadHelper.create("reloader", false, () -> {
                    try {
                        switchTo(BACKGROUND);
                        CURRENT.finalTeardown();
                        CURRENT.initialSetup();
                        switchTo(GUI);
                    } catch (Throwable t) {
                        ErrorEvent.fromThrowable(t).build().handle();
                        OperationMode.halt(1);
                    }
                })
                .start();
    }

    private static synchronized void set(OperationMode newMode) {
        if (CURRENT == null && newMode == null) {
            return;
        }

        if (CURRENT != null && CURRENT.equals(newMode)) {
            return;
        }

        try {
            if (CURRENT == null) {
                CURRENT = newMode;
                newMode.initialSetup();
            } else if (newMode == null) {
                shutdown(false, false);
            } else {
                var cur = CURRENT;
                cur.onSwitchFrom();
                CURRENT = newMode;
                newMode.onSwitchTo();
            }
        } catch (Throwable ex) {
            ErrorEvent.fromThrowable(ex).terminal(true).build().handle();
        }
    }

    public static OperationMode get() {
        return CURRENT;
    }

    public static boolean isInShutdownHook() {
        return inShutdownHook;
    }

    public abstract boolean isSupported();

    public abstract String getId();

    public abstract void onSwitchTo();

    public abstract void onSwitchFrom() throws Throwable;

    public abstract void initialSetup() throws Throwable;

    public abstract void finalTeardown() throws Throwable;

    public abstract ErrorHandler getErrorHandler();
}
