package io.xpipe.app.beacon.impl;

import com.sun.net.httpserver.HttpExchange;
import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.beacon.BeaconClientException;
import io.xpipe.beacon.BeaconServerException;
import io.xpipe.beacon.api.DaemonFocusExchange;

import java.io.IOException;

public class DaemonFocusExchangeImpl extends DaemonFocusExchange {


@Override
public Object handle(HttpExchange exchange, Request msg) throws IOException, BeaconClientException, BeaconServerException {

    OperationMode.switchUp(OperationMode.map(msg.getMode()));
    return Response.builder().build();
}
}
