package io.xpipe.app.core;

import io.xpipe.app.beacon.AppBeaconServer;
import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.beacon.BeaconClient;
import io.xpipe.beacon.BeaconClientInformation;
import io.xpipe.beacon.BeaconServer;
import io.xpipe.beacon.api.DaemonFocusExchange;
import io.xpipe.beacon.api.DaemonOpenExchange;
import io.xpipe.core.process.OsType;
import io.xpipe.core.util.XPipeInstallation;

import java.awt.*;
import java.util.List;

public class AppInstance {

    public static void init() {
        checkStart(0);
    }

    private static void checkStart(int attemptCounter) {
        var port = AppBeaconServer.get().getPort();
        var reachable = BeaconServer.isReachable(port);
        if (!reachable) {
            // Even in case we are unable to reach another beacon server
            // there might be another instance running, for example
            // starting up or listening on another port
            if (!AppDataLock.lock()) {
                TrackEvent.info(
                        "Data directory " + AppProperties.get().getDataDir().toString()
                                + " is already locked. Is another instance running?");
                OperationMode.halt(1);
            }

            // We are good to start up!
            return;
        }

        var client = BeaconClient.tryEstablishConnection(
                port, BeaconClientInformation.Daemon.builder().build());
        if (client.isEmpty()) {
            // If an instance is running as another user, we cannot connect to it as the xpipe_auth file is inaccessible
            // Therefore the beacon client is not present.
            // We still should check whether it is somehow occupied, otherwise beacon server startup will fail
            TrackEvent.info(
                    "Another instance is already running on this port as another user or is not reachable. Quitting ...");
            OperationMode.halt(1);
            return;
        }

        try {
            var inputs = AppProperties.get().getArguments().getOpenArgs();
            // Assume that we want to open the GUI if we launched again
            client.get().performRequest(DaemonFocusExchange.Request.builder().build());
            if (!inputs.isEmpty()) {
                client.get()
                        .performRequest(DaemonOpenExchange.Request.builder()
                                .arguments(inputs)
                                .build());
            }
        } catch (Exception ex) {
            // Wait until shutdown has completed
            if (ex.getMessage() != null
                    && ex.getMessage().contains("Daemon is currently in shutdown")
                    && attemptCounter < 10) {
                ThreadHelper.sleep(1000);
                checkStart(++attemptCounter);
                return;
            }

            var cli = XPipeInstallation.getLocalDefaultCliExecutable();
            ErrorEventFactory.fromThrowable(
                            "Unable to connect to existing running daemon instance as it did not respond."
                                    + " Either try to kill the process xpiped manually or use the command \""
                                    + cli
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
                    ErrorEventFactory.fromThrowable(ex).expected().omit().handle();
                }
            });
            ThreadHelper.sleep(1000);
        }
        TrackEvent.info("Another instance is already running on this port. Quitting ...");
        OperationMode.halt(1);
    }
}
