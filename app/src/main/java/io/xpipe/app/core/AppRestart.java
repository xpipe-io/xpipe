package io.xpipe.app.core;

import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.process.ShellDialect;
import io.xpipe.app.process.ShellDialects;
import io.xpipe.app.util.LocalShell;
import io.xpipe.core.OsType;

import java.util.List;

public class AppRestart {

    private static String createTerminalLaunchCommand(List<String> arguments, ShellDialect dialect) {
        var loc = AppProperties.get().isDevelopmentEnvironment()
                ? AppInstallation.ofDefault()
                : AppInstallation.ofCurrent();
        var suffix = (arguments.size() > 0 ? " " + String.join(" ", arguments) : "");
        if (OsType.getLocal() == OsType.LINUX) {
            var exec = loc.getCliExecutablePath();
            return "\"" + exec + "\" open" + suffix;
        } else if (OsType.getLocal() == OsType.MACOS) {
            var exec = loc.getCliExecutablePath();
            return "\"" + exec + "\" open" + suffix;
        } else {
            var exe = loc.getDaemonDebugScriptPath();
            if (ShellDialects.isPowershell(dialect)) {
                var escapedList =
                        arguments.stream().map(s -> s.replaceAll("\"", "`\"")).toList();
                var argumentList = String.join(" ", escapedList);
                return "Start-Process -FilePath \"" + exe + "\" -ArgumentList \"" + argumentList + "\"";
            } else {
                var base = "\"" + exe + "\"" + suffix;
                return "start \"\" " + base;
            }
        }
    }

    private static String createBackgroundLaunchCommand(List<String> arguments, ShellDialect dialect) {
        var loc = AppProperties.get().isDevelopmentEnvironment()
                ? AppInstallation.ofDefault()
                : AppInstallation.ofCurrent();
        var suffix = (arguments.size() > 0 ? " " + String.join(" ", arguments) : "");
        if (OsType.getLocal() == OsType.LINUX) {
            return "nohup \"" + loc.getDaemonExecutablePath() + "\"" + suffix + " </dev/null >/dev/null 2>&1 & disown";
        } else if (OsType.getLocal() == OsType.MACOS) {
            return "(sleep 1;open \"" + loc.getBaseInstallationPath() + "\" --args" + suffix + " </dev/null &>/dev/null) & disown";
        } else {
            var exe = loc.getDaemonExecutablePath();
            if (ShellDialects.isPowershell(dialect)) {
                var escapedList =
                        arguments.stream().map(s -> s.replaceAll("\"", "`\"")).toList();
                var argumentList = String.join(" ", escapedList);
                return "Start-Process -FilePath \"" + exe + "\" -ArgumentList \"" + argumentList + "\"";
            } else {
                var base = "\"" + exe + "\"" + suffix;
                return "start \"\" " + base;
            }
        }
    }

    public static String getBackgroundRestartCommand(ShellDialect dialect) {
        var dataDir = AppProperties.get().getDataDir();
        var exec = createBackgroundLaunchCommand(
                List.of(
                        "-Dio.xpipe.app.mode=gui",
                        "-Dio.xpipe.app.acceptEula=true",
                        "-Dio.xpipe.app.dataDir=\"" + dataDir + "\"",
                        "-Dio.xpipe.app.restarted=true"),
                dialect);
        return exec;
    }

    public static String getBackgroundRestartCommand() {
        return getBackgroundRestartCommand(ProcessControlProvider.get().getEffectiveLocalDialect());
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
        return getTerminalRestartCommand(ProcessControlProvider.get().getEffectiveLocalDialect());
    }

    public static void restart() {
        OperationMode.executeAfterShutdown(() -> {
            try (var sc = LocalShell.getShell().start()) {
                sc.command(getBackgroundRestartCommand()).execute();
            }
        });
    }
}
