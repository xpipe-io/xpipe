package io.xpipe.app.beacon.impl;

import com.sun.net.httpserver.HttpExchange;
import io.xpipe.app.util.TerminalLauncherManager;
import io.xpipe.beacon.BeaconClientException;
import io.xpipe.beacon.BeaconServerException;
import io.xpipe.beacon.api.TerminalWaitExchange;

import java.io.IOException;

public class TerminalWaitExchangeImpl extends TerminalWaitExchange {
    @Override
    public Object handle(HttpExchange exchange, Request msg) throws IOException, BeaconClientException, BeaconServerException {
        TerminalLauncherManager.waitForCompletion(msg.getRequest());
        return Response.builder().build();
    }
}
