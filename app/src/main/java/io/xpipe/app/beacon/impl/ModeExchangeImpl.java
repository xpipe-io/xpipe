package io.xpipe.app.beacon.impl;

import com.sun.net.httpserver.HttpExchange;
import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.beacon.BeaconClientException;
import io.xpipe.beacon.BeaconServerException;
import io.xpipe.beacon.api.ModeExchange;

import java.io.IOException;

public class ModeExchangeImpl extends ModeExchange {
    @Override
    public Object handle(HttpExchange exchange, Request msg) throws IOException, BeaconClientException, BeaconServerException {
        // Wait for startup
        while (OperationMode.get() == null) {
            ThreadHelper.sleep(100);
        }

        var mode = OperationMode.map(msg.getMode());
        if (!mode.isSupported()) {
            throw new BeaconClientException("Unsupported mode: " + msg.getMode().getDisplayName() + ". Supported: "
                    + String.join(
                    ", ",
                    OperationMode.getAll().stream()
                            .filter(OperationMode::isSupported)
                            .map(OperationMode::getId)
                            .toList()));
        }

        OperationMode.switchToSyncIfPossible(mode);
        return ModeExchange.Response.builder()
                .usedMode(OperationMode.map(OperationMode.get()))
                .build();
    }
}
