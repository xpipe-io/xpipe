package io.xpipe.app.beacon.impl;

import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.beacon.BeaconClientException;
import io.xpipe.beacon.api.DaemonModeExchange;

import com.sun.net.httpserver.HttpExchange;

public class DaemonModeExchangeImpl extends DaemonModeExchange {
    @Override
    public Object handle(HttpExchange exchange, Request msg) throws BeaconClientException {
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
        return DaemonModeExchange.Response.builder()
                .usedMode(OperationMode.map(OperationMode.get()))
                .build();
    }

    @Override
    public boolean requiresEnabledApi() {
        return false;
    }
}
