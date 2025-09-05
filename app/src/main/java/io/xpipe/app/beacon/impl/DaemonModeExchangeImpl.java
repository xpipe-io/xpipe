package io.xpipe.app.beacon.impl;

import io.xpipe.app.core.mode.AppOperationMode;
import io.xpipe.beacon.BeaconClientException;
import io.xpipe.beacon.api.DaemonModeExchange;

import com.sun.net.httpserver.HttpExchange;

public class DaemonModeExchangeImpl extends DaemonModeExchange {
    @Override
    public Object handle(HttpExchange exchange, Request msg) throws BeaconClientException {
        var mode = AppOperationMode.map(msg.getMode());
        if (!mode.isSupported()) {
            throw new BeaconClientException("Unsupported mode: " + msg.getMode().getDisplayName()
                    + ". Supported: "
                    + String.join(
                            ", ",
                            AppOperationMode.getAll().stream()
                                    .filter(AppOperationMode::isSupported)
                                    .map(AppOperationMode::getId)
                                    .toList()));
        }

        AppOperationMode.switchToSyncIfPossible(mode);
        return DaemonModeExchange.Response.builder().usedMode(msg.getMode()).build();
    }

    @Override
    public boolean requiresEnabledApi() {
        return false;
    }
}
