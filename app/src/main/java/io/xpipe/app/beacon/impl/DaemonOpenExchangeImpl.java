package io.xpipe.app.beacon.impl;

import io.xpipe.app.core.launcher.LauncherInput;
import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.util.PlatformInit;
import io.xpipe.app.util.PlatformState;
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
            LauncherInput.handle(msg.getArguments());
        }
        return Response.builder().build();
    }

    @Override
    public boolean requiresEnabledApi() {
        return false;
    }
}
