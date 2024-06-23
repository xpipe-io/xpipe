package io.xpipe.app.beacon.impl;

import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.beacon.api.DaemonStatusExchange;

import com.sun.net.httpserver.HttpExchange;

public class DaemonStatusExchangeImpl extends DaemonStatusExchange {

    @Override
    public boolean requiresCompletedStartup() {
        return false;
    }

    @Override
    public Object handle(HttpExchange exchange, Request body) {
        String mode;
        if (OperationMode.get() == null) {
            mode = "none";
        } else {
            mode = OperationMode.get().getId();
        }

        return Response.builder().mode(mode).build();
    }
}
