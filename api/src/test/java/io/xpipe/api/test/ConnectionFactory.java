package io.xpipe.api.test;

import io.xpipe.api.connector.XPipeConnection;
import io.xpipe.beacon.BeaconClient;
import io.xpipe.beacon.BeaconServer;

public class ConnectionFactory {

    private static boolean alreadyStarted;

    public static void start() throws Exception {
        if (BeaconServer.isRunning()) {
            alreadyStarted = true;
            return;
        }

        if (!BeaconServer.tryStart()) {
            throw new AssertionError();
        }

        XPipeConnection.waitForStartup();
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

        var client = new BeaconClient();
        if (!BeaconServer.tryStop(client)) {
            throw new AssertionError();
        }
        XPipeConnection.waitForShutdown();
    }
}
