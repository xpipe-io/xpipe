package io.xpipe.app.beacon.impl;

import com.sun.net.httpserver.HttpExchange;
import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.launcher.LauncherInput;
import io.xpipe.app.util.PlatformState;
import io.xpipe.beacon.BeaconServerException;
import io.xpipe.beacon.api.DaemonOpenExchange;

public class DaemonOpenExchangeImpl extends DaemonOpenExchange {

    @Override
    public Object handle(HttpExchange exchange, Request msg)
            throws BeaconServerException {
        if (msg.getArguments().isEmpty()) {
            if (!OperationMode.switchToSyncIfPossible(OperationMode.GUI)) {
                throw new BeaconServerException(PlatformState.getLastError());
            }
        }

        LauncherInput.handle(msg.getArguments());
        return Response.builder().build();
    }
}
