package io.xpipe.app.beacon.impl;

import io.xpipe.app.terminal.TerminalLauncherManager;
import io.xpipe.beacon.BeaconClientException;
import io.xpipe.beacon.BeaconServerException;
import io.xpipe.beacon.api.TerminalLaunchExchange;

import com.sun.net.httpserver.HttpExchange;

public class TerminalLaunchExchangeImpl extends TerminalLaunchExchange {
    @Override
    public Object handle(HttpExchange exchange, Request msg) throws BeaconClientException, BeaconServerException {
        var r = TerminalLauncherManager.launchExchange(msg.getRequest());
        return Response.builder().targetFile(r).build();
    }

    @Override
    public boolean requiresEnabledApi() {
        return false;
    }
}
