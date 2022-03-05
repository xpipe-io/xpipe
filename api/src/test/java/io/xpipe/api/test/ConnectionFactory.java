package io.xpipe.api.test;

import io.xpipe.api.connector.XPipeConnection;
import io.xpipe.beacon.BeaconClient;
import io.xpipe.beacon.BeaconServer;

public class ConnectionFactory {

    public static void start() throws Exception {
        if (!BeaconServer.tryStart()) {
            throw new AssertionError();
        }

        XPipeConnection.waitForStartup();
        if (!BeaconServer.isRunning()) {
            throw new AssertionError();
        }
    }

    public static void stop() throws Exception {
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
