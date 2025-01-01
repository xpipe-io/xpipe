package io.xpipe.app.beacon.impl;

import io.xpipe.app.core.AppOpenArguments;
import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.util.PlatformInit;
import io.xpipe.beacon.BeaconServerException;
import io.xpipe.beacon.api.DaemonOpenExchange;

import com.sun.net.httpserver.HttpExchange;

public class DaemonOpenExchangeImpl extends DaemonOpenExchange {

    @Override
    public Object handle(HttpExchange exchange, Request msg) throws BeaconServerException {
        if (msg.getArguments().isEmpty()) {
            var err = PlatformInit.getError();
            if (err != null) {
                throw new BeaconServerException(err);
            }

            OperationMode.switchToAsync(OperationMode.GUI);
        } else {
            AppOpenArguments.handle(msg.getArguments());
        }
        return Response.builder().build();
    }

    @Override
    public boolean requiresEnabledApi() {
        return false;
    }

    @Override
    public boolean requiresCompletedStartup() {
        return false;
    }
}
