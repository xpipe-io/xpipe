package io.xpipe.app.beacon.impl;

import io.xpipe.app.util.TerminalLauncherManager;
import io.xpipe.beacon.BeaconClientException;
import io.xpipe.beacon.api.TerminalLaunchExchange;

import com.sun.net.httpserver.HttpExchange;

public class TerminalLaunchExchangeImpl extends TerminalLaunchExchange {
    @Override
    public Object handle(HttpExchange exchange, Request msg) throws BeaconClientException {
        var r = TerminalLauncherManager.performLaunch(msg.getRequest());
        return Response.builder().targetFile(r).build();
    }
}
