package io.xpipe.app.launcher;

import io.xpipe.app.beacon.AppBeaconServer;
import io.xpipe.app.core.AppDataLock;
import io.xpipe.app.core.AppLogs;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.LogErrorHandler;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.beacon.BeaconClient;
import io.xpipe.beacon.BeaconClientInformation;
import io.xpipe.beacon.api.DaemonFocusExchange;
import io.xpipe.beacon.api.DaemonOpenExchange;
import io.xpipe.core.process.OsType;
import io.xpipe.core.util.XPipeDaemonMode;
import io.xpipe.core.util.XPipeInstallation;

import lombok.SneakyThrows;
import picocli.CommandLine;

import java.awt.*;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

@CommandLine.Command(
        header = "Launches the XPipe daemon.",
        sortOptions = false,
        showEndOfOptionsDelimiterInUsageHelp = true)
public class LauncherCommand implements Callable<Integer> {

    @CommandLine.Parameters(paramLabel = "<input>")
    final List<String> inputs = List.of();

    @CommandLine.Option(
            names = {"--mode"},
            description = "The mode to launch the daemon in or switch too",
            paramLabel = "<mode id>",
            converter = LauncherModeConverter.class)
    XPipeDaemonMode mode;

    public static void runLauncher(String[] args) {
        TrackEvent.builder()
                .type("debug")
                .message("Launcher received commands: " + Arrays.asList(args))
                .handle();

        var cmd = new CommandLine(new LauncherCommand());
        cmd.setExecutionExceptionHandler((ex, commandLine, parseResult) -> {
            var event = ErrorEvent.fromThrowable(ex).term().build();
            // Print error in case we launched from the command-line
            new LogErrorHandler().handle(event);
            event.handle();
            return 1;
        });
        cmd.setParameterExceptionHandler((ex, args1) -> {
            var event = ErrorEvent.fromThrowable(ex).term().expected().build();
            // Print error in case we launched from the command-line
            new LogErrorHandler().handle(event);
            event.handle();
            return 1;
        });

        // Use original output streams for command output
        cmd.setOut(new PrintWriter(AppLogs.get().getOriginalSysOut()));
        cmd.setErr(new PrintWriter(AppLogs.get().getOriginalSysErr()));

        try {
            cmd.parseArgs(args);
            cmd.execute(args);
        } catch (Throwable t) {
            var e = ErrorEvent.fromThrowable(t).term().build();
            // Print error in case we launched from the command-line
            new LogErrorHandler().handle(e);
            e.handle();
        }
    }

    private void checkStart() {
        var port = AppBeaconServer.get().getPort();
        var client = BeaconClient.tryEstablishConnection(
                port, BeaconClientInformation.Daemon.builder().build());
        if (client.isPresent()) {
            try {
                client.get()
                        .performRequest(DaemonFocusExchange.Request.builder()
                                .mode(getEffectiveMode())
                                .build());
                if (!inputs.isEmpty()) {
                    client.get()
                            .performRequest(DaemonOpenExchange.Request.builder()
                                    .arguments(inputs)
                                    .build());
                }
            } catch (Exception ex) {
                var cli = XPipeInstallation.getLocalDefaultCliExecutable();
                ErrorEvent.fromThrowable(
                                "Unable to connect to existing running daemon instance as it did not respond."
                                        + " Either try to kill the process xpiped manually or use the command \"" + cli
                                        + "\" daemon stop --force.",
                                ex)
                        .term()
                        .expected()
                        .handle();
            }

            if (OsType.getLocal().equals(OsType.MACOS)) {
                Desktop.getDesktop().setOpenURIHandler(e -> {
                    try {
                        client.get()
                                .performRequest(DaemonOpenExchange.Request.builder()
                                        .arguments(List.of(e.getURI().toString()))
                                        .build());
                    } catch (Exception ex) {
                        ErrorEvent.fromThrowable(ex).expected().omit().handle();
                    }
                });
                ThreadHelper.sleep(1000);
            }
            TrackEvent.info("Another instance is already running on this port. Quitting ...");
            OperationMode.halt(1);
        }

        // Even in case we are unable to reach another beacon server
        // there might be another instance running, for example
        // starting up or listening on another port
        if (!AppDataLock.lock()) {
            TrackEvent.info("Data directory " + AppProperties.get().getDataDir().toString()
                    + " is already locked. Is another instance running?");
            OperationMode.halt(1);
        }

        // If an instance is running as another user, we cannot connect to it as the xpipe_auth file is inaccessible
        // Therefore the beacon client is not present.
        // We still should check whether it is somehow occupied, otherwise beacon server startup will fail
        if (BeaconClient.isOccupied(port)) {
            TrackEvent.info("Another instance is already running on this port as another user. Quitting ...");
            OperationMode.halt(1);
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

        return AppPrefs.get() != null
                ? AppPrefs.get().startupBehaviour().getValue().getMode()
                : XPipeDaemonMode.GUI;
    }

    @Override
    @SneakyThrows
    public Integer call() {
        checkStart();

        // Initialize base mode first to have access to the preferences to determine effective mode
        OperationMode.switchToSyncOrThrow(OperationMode.BACKGROUND);

        var effective = OperationMode.map(getEffectiveMode());
        if (effective != OperationMode.BACKGROUND) {
            OperationMode.switchToSyncOrThrow(effective);
        }

        LauncherInput.handle(inputs);
        return 0;
    }
}
