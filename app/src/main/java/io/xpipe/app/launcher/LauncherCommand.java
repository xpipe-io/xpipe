package io.xpipe.app.launcher;

import io.xpipe.app.core.AppLock;
import io.xpipe.app.core.AppLogs;
import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.issue.BasicErrorHandler;
import io.xpipe.beacon.BeaconServer;
import io.xpipe.beacon.exchange.FocusExchange;
import io.xpipe.beacon.exchange.OpenExchange;
import io.xpipe.core.process.OsType;
import io.xpipe.core.util.XPipeDaemonMode;
import io.xpipe.extension.event.ErrorEvent;
import io.xpipe.extension.event.TrackEvent;
import io.xpipe.extension.util.ThreadHelper;
import picocli.CommandLine;

import java.awt.*;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

@CommandLine.Command(
        header = "Launches the X-Pipe daemon.",
        sortOptions = false,
        showEndOfOptionsDelimiterInUsageHelp = true)
public class LauncherCommand implements Callable<Integer> {

    @CommandLine.Option(
            names = {"--mode"},
            description = "The mode to launch the daemon in or switch too",
            paramLabel = "<mode id>",
            converter = LauncherModeConverter.class)
    XPipeDaemonMode mode;

    @CommandLine.Parameters(paramLabel = "<input>")
    List<String> inputs = List.of();

    public static void runLauncher(String[] args) {
        event("Launcher received commands: " + Arrays.asList(args));

        var cmd = new CommandLine(new LauncherCommand());
        cmd.setExecutionExceptionHandler((ex, commandLine, parseResult) -> {
            new BasicErrorHandler()
                    .handle(ErrorEvent.fromThrowable("Launcher command error occurred", ex)
                            .build());
            OperationMode.halt(1);
            return 1;
        });
        cmd.setParameterExceptionHandler((ex, args1) -> {
            new BasicErrorHandler()
                    .handle(ErrorEvent.fromThrowable("Launcher parameter error occurred", ex)
                            .build());
            OperationMode.halt(1);
            return 1;
        });

        // Use original output streams for command output
        cmd.setOut(new PrintWriter(AppLogs.get().getOriginalSysOut()));
        cmd.setErr(new PrintWriter(AppLogs.get().getOriginalSysErr()));

        cmd.parseArgs(args);
        cmd.execute(args);
    }

    private static TrackEvent.TrackEventBuilder event(String msg) {
        return TrackEvent.builder().category("launcher").type("debug").message(msg);
    }

    private void checkStart() {
        if (BeaconServer.isRunning()) {
            try (var con = new LauncherConnection()) {
                con.constructSocket();
                con.performSimpleExchange(
                        FocusExchange.Request.builder().mode(getEffectiveMode()).build());
                if (inputs.size() > 0) {
                    con.performSimpleExchange(
                            OpenExchange.Request.builder().arguments(inputs).build());
                }

                if (OsType.getLocal().equals(OsType.MACOS)) {
                    Desktop.getDesktop().setOpenURIHandler(e -> {
                        con.performSimpleExchange(OpenExchange.Request.builder()
                                .arguments(List.of(e.getURI().toString()))
                                .build());
                    });
                    ThreadHelper.sleep(1000);
                }
            }
            TrackEvent.info("Another instance is already running on this port. Quitting ...");
            OperationMode.halt(0);
        }

        // Even in case we are unable to reach another beacon server
        // there might be another instance running, for example
        // starting up or listening on another port
        if (!AppLock.lock()) {
            TrackEvent.info("Data directory is already locked. Quitting ...");
            OperationMode.halt(0);
        }
    }

    private XPipeDaemonMode getEffectiveMode() {
        if (mode != null) {
            return mode;
        }

        var opModeName = System.getProperty(OperationMode.MODE_PROP) != null
                ? System.getProperty(OperationMode.MODE_PROP)
                : null;
        if (opModeName != null) {
            return XPipeDaemonMode.get(opModeName);
        }

        return XPipeDaemonMode.GUI;
    }

    @Override
    public Integer call() {
        checkStart();
        OperationMode.switchTo(OperationMode.map(getEffectiveMode()));
        LauncherInput.handle(inputs);

        // URL open operations have to be handled in a special way on macOS!
        if (OsType.getLocal().equals(OsType.MACOS)) {
            Desktop.getDesktop().setOpenURIHandler(e -> {
                LauncherInput.handle(List.of(e.getURI().toString()));
            });
        }

        return 0;
    }
}
