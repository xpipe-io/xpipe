package io.xpipe.api.test;

import io.xpipe.beacon.BeaconClient;
import io.xpipe.beacon.BeaconServer;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class XPipeConfig implements BeforeAllCallback, ExtensionContext.Store.CloseableResource {

    private static boolean started = false;

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        if (!started) {
            started = true;
            if (BeaconServer.tryStart()) {
                throw new AssertionError();
            }
        }
    }

    @Override
    public void close() throws Exception {
        var client = new BeaconClient();
        if (BeaconServer.tryStop(client)) {
            throw new AssertionError();
        }
    }
}
