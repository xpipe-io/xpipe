package io.xpipe.api.util;

import io.xpipe.api.connector.XPipeConnection;
import io.xpipe.beacon.BeaconClient;
import io.xpipe.beacon.BeaconServer;

public class XPipeDaemonController {

    private static boolean alreadyStarted;

    public static void start() throws Exception {
        if (BeaconServer.isRunning()) {
            alreadyStarted = true;
            return;
        }

        Process process = null;
        if ((process = BeaconServer.tryStartCustom()) != null) {
        } else {
            if ((process = BeaconServer.tryStart()) == null) {
                throw new AssertionError();
            }
        }

        XPipeConnection.waitForStartup(process).orElseThrow();
        if (!BeaconServer.isRunning()) {
            throw new AssertionError();
        }
    }

    public static void stop() throws Exception {
        if (alreadyStarted) {
            return;
        }

        if (!BeaconServer.isRunning()) {
            return;
        }

        var client = BeaconClient.connect(BeaconClient.ApiClientInformation.builder().version("?").language("Java API Test").build());
        if (!BeaconServer.tryStop(client)) {
            throw new AssertionError();
        }
        XPipeConnection.waitForShutdown();
    }
}
