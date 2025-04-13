package io.xpipe.app.core;

import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.util.LocalShell;
import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellDialect;
import io.xpipe.core.process.ShellDialects;
import io.xpipe.core.store.FileNames;
import io.xpipe.core.util.XPipeDaemonMode;
import io.xpipe.core.util.XPipeInstallation;

import java.util.ArrayList;
import java.util.List;

public class AppRestart {

    private static String createExternalAsyncLaunchCommand(XPipeDaemonMode mode, List<String> arguments, ShellDialect dialect) {
        var loc = AppProperties.get().isDevelopmentEnvironment()
                ? XPipeInstallation.getLocalDefaultInstallationBasePath()
                : XPipeInstallation.getCurrentInstallationBasePath().toString();
        var suffix = (arguments != null ? " " + String.join(" ", arguments) : "");
        var modeOption = mode != null ? " --mode " + mode.getDisplayName() : "";
        if (OsType.getLocal().equals(OsType.LINUX)) {
            return "nohup \"" + loc + "/bin/xpiped\"" + modeOption + suffix + " & disown";
        } else if (OsType.getLocal().equals(OsType.MACOS)) {
            return "(sleep 1;open \"" + loc + "\" --args" + modeOption + suffix
                    + "</dev/null &>/dev/null) & disown";
        } else {
            var exe = FileNames.join(loc, XPipeInstallation.getDaemonExecutablePath(OsType.getLocal()));
            if (ShellDialects.isPowershell(dialect)) {
                var list = new ArrayList<String>();
                if (mode != null) {
                    list.add("--mode");
                    list.add(mode.getDisplayName());
                }
                list.addAll(arguments);
                var escapedList = list.stream().map(s -> s.replaceAll("\"", "`\"")).toList();
                var argumentList = String.join(" ", escapedList);
                return "Start-Process -FilePath \"" + exe + "\" -ArgumentList \"" + argumentList + "\"";
            } else {
                var base = "\"" + FileNames.join(loc, XPipeInstallation.getDaemonExecutablePath(OsType.getLocal()))
                        + "\"" + modeOption + suffix;
                return "start \"\" " + base;
            }
        }
    }

    public static String getRestartCommand(ShellDialect dialect) {
        var dataDir = AppProperties.get().getDataDir();
        // We have to quote the arguments like this to make it work in PowerShell as well
        var exec = createExternalAsyncLaunchCommand(
                XPipeDaemonMode.GUI,
                List.of(
                        "-Dio.xpipe.app.acceptEula=true",
                        "-Dio.xpipe.app.dataDir=\"" + dataDir + "\"",
                        "-Dio.xpipe.app.restarted=true"),
                dialect);
        return exec;
    }

    public static String getRestartCommand() {
        return getRestartCommand(ProcessControlProvider.get().getEffectiveLocalDialect());
    }

    public static void restart() {
        OperationMode.executeAfterShutdown(() -> {
            try (var sc = LocalShell.getShell().start()) {
                sc.command(getRestartCommand()).execute();
            }
        });
    }
}
