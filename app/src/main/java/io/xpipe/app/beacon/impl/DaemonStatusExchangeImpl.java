package io.xpipe.app.beacon.impl;


import com.sun.net.httpserver.HttpExchange;
import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.beacon.BeaconClientException;
import io.xpipe.beacon.BeaconServerException;
import io.xpipe.beacon.api.DaemonStatusExchange;

import java.io.IOException;

public class DaemonStatusExchangeImpl extends DaemonStatusExchange {

    @Override
    public Object handle(HttpExchange exchange, Request body) throws IOException, BeaconClientException, BeaconServerException {
        String mode;
        if (OperationMode.get() == null) {
            mode = "none";
        } else {
            mode = OperationMode.get().getId();
        }

        return Response.builder().mode(mode).build();
    }
}
