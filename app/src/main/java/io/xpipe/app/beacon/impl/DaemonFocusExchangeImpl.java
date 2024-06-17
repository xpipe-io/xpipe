package io.xpipe.app.beacon.impl;

import com.sun.net.httpserver.HttpExchange;
import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.beacon.api.DaemonFocusExchange;

public class DaemonFocusExchangeImpl extends DaemonFocusExchange {

    @Override
    public Object handle(HttpExchange exchange, Request msg) {
        OperationMode.switchUp(OperationMode.map(msg.getMode()));
        return Response.builder().build();
    }
}
