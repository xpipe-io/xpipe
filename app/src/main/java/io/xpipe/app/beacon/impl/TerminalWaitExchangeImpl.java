package io.xpipe.app.beacon.impl;

import io.xpipe.app.terminal.TerminalLauncherManager;
import io.xpipe.app.terminal.TerminalView;
import io.xpipe.beacon.BeaconClientException;
import io.xpipe.beacon.BeaconServerException;
import io.xpipe.beacon.api.TerminalWaitExchange;

import com.sun.net.httpserver.HttpExchange;

public class TerminalWaitExchangeImpl extends TerminalWaitExchange {
    @Override
    public Object handle(HttpExchange exchange, Request msg) throws BeaconClientException, BeaconServerException {
        TerminalView.get().open(msg.getRequest(), msg.getPid());
        TerminalLauncherManager.waitExchange(msg.getRequest(), msg.getPid());
        return Response.builder().build();
    }

    @Override
    public boolean requiresEnabledApi() {
        return false;
    }
}
