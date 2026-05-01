package io.xpipe.app.core;

import io.xpipe.app.core.mode.AppOperationMode;
import io.xpipe.app.process.*;
import io.xpipe.app.update.AppDistributionType;
import io.xpipe.core.OsType;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class AppRestart {

    private static String createTerminalLaunchCommand(List<String> arguments, ShellDialect dialect) {
        var loc = AppProperties.get().isDevelopmentEnvironment()
                ? AppInstallation.ofDefault()
                : AppInstallation.ofCurrent();
        if (AppDistributionType.get() == AppDistributionType.APP_IMAGE) {
            var exec = System.getenv("APPIMAGE");
            var b = CommandBuilder.of().addQuoted(exec).addAll(arguments);
            var async = dialect.launchAsync(b, true);
            return async.buildSimple();
        } else if (OsType.ofLocal() == OsType.LINUX) {
            var exec = loc.getCliExecutablePath();
            var b = CommandBuilder.of().addFile(exec).add("open").addAll(arguments);
            return b.buildSimple();
        } else if (OsType.ofLocal() == OsType.MACOS) {
            var exec = loc.getCliExecutablePath();
            var b = CommandBuilder.of().addFile(exec).add("open").addAll(arguments);
            return b.buildSimple();
        } else {
            var exe = loc.getDaemonExecutablePath();
            var b = CommandBuilder.of().addFile(exe).addAll(arguments);
            var async = dialect.launchAsync(b, true);
            return async.buildSimple();
        }
    }

    private static String createBackgroundLaunchCommand(List<String> arguments, ShellDialect dialect) {
        var loc = AppProperties.get().isDevelopmentEnvironment()
                ? AppInstallation.ofDefault()
                : AppInstallation.ofCurrent();
        if (AppDistributionType.get() == AppDistributionType.APP_IMAGE) {
            var exec = System.getenv("APPIMAGE");
            var b = CommandBuilder.of().addQuoted(exec).addAll(arguments);
            var async = dialect.launchAsync(b, true);
            return async.buildSimple();
        } else if (OsType.ofLocal() == OsType.LINUX) {
            var exec = loc.getDaemonExecutablePath();
            var b = CommandBuilder.of().addFile(exec).addAll(arguments);
            var async = dialect.launchAsync(b, true);
            return async.buildSimple();
        } else if (OsType.ofLocal() == OsType.MACOS) {
            var exec = loc.getDaemonExecutablePath();
            var b = CommandBuilder.of()
                    .add("open")
                    .addFile(exec)
                    .addIf(!arguments.isEmpty(), "--args")
                    .addAll(arguments);
            return b.buildSimple();
        } else {
            var exe = loc.getDaemonExecutablePath();
            var b = CommandBuilder.of().addFile(exe).addAll(arguments);
            var async = dialect.launchAsync(b, true);
            return async.buildSimple();
        }
    }

    public static String getBackgroundRestartCommand(Path dataDir, String user, ShellDialect dialect) {
        var l = new ArrayList<String>();
        l.addAll(List.of(
                "-Dio.xpipe.app.mode=gui",
                "-Dio.xpipe.app.acceptEula=true",
                "-Dio.xpipe.app.dataDir=\"" + dataDir + "\"",
                "-Dio.xpipe.app.restarted=true"));
        if (user != null) {
            l.add("-Dio.xpipe.app.login=\"" + user + "\"");
        }
        var exec = createBackgroundLaunchCommand(l, dialect);
        return exec;
    }

    public static String getBackgroundRestartCommand() {
        return getBackgroundRestartCommand(AppProperties.get().getDataDir(), null, LocalShell.getDialect());
    }

    public static String getTerminalRestartCommand(ShellDialect dialect) {
        var dataDir = AppProperties.get().getDataDir();
        var exec = createTerminalLaunchCommand(
                List.of(
                        "-Dio.xpipe.app.mode=gui",
                        "-Dio.xpipe.app.acceptEula=true",
                        "-Dio.xpipe.app.dataDir=\"" + dataDir + "\"",
                        "-Dio.xpipe.app.restarted=true"),
                dialect);
        return exec;
    }

    public static String getTerminalRestartCommand() {
        return getTerminalRestartCommand(LocalShell.getDialect());
    }

    public static void restart() {
        AppOperationMode.executeAfterShutdown(() -> {
            try (var sc = LocalShell.getShell().start()) {
                sc.command(getBackgroundRestartCommand()).execute();
            }
        });
    }

    public static void restart(Path dataDir) {
        AppOperationMode.executeAfterShutdown(() -> {
            try (var sc = LocalShell.getShell().start()) {
                sc.command(getBackgroundRestartCommand(dataDir, null, sc.getShellDialect()))
                        .execute();
            }
        });
    }
}
