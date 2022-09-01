package io.xpipe.extension.test;

import io.xpipe.api.connector.XPipeConnection;
import io.xpipe.beacon.BeaconClient;
import io.xpipe.beacon.BeaconServer;
import io.xpipe.core.charsetter.Charsetter;
import io.xpipe.core.charsetter.CharsetterContext;
import io.xpipe.core.util.JacksonHelper;
import io.xpipe.extension.DataSourceProviders;

public class ExtensionTestConnector {

    private static boolean alreadyStarted;

    public static void start() throws Exception {
        DataSourceProviders.init(ModuleLayer.boot());
        JacksonHelper.initClassBased();
        Charsetter.init(CharsetterContext.empty());

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

        var client = new BeaconClient();
        if (!BeaconServer.tryStop(client)) {
            throw new AssertionError();
        }
        XPipeConnection.waitForShutdown();
    }
}
