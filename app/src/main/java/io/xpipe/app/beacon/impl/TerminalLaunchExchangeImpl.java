package io.xpipe.app.beacon.impl;

import com.sun.net.httpserver.HttpExchange;
import io.xpipe.app.util.TerminalLauncherManager;
import io.xpipe.beacon.BeaconClientException;
import io.xpipe.beacon.BeaconServerException;
import io.xpipe.beacon.api.TerminalLaunchExchange;

import java.io.IOException;

public class TerminalLaunchExchangeImpl extends TerminalLaunchExchange {
    @Override
    public Object handle(HttpExchange exchange, Request msg) throws IOException, BeaconClientException, BeaconServerException {
        var r = TerminalLauncherManager.performLaunch(msg.getRequest());
        return Response.builder().targetFile(r).build();
    }
}
