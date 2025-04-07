package io.xpipe.app.beacon.impl;

import com.sun.net.httpserver.HttpExchange;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.terminal.TerminalLauncherManager;
import io.xpipe.app.terminal.TerminalView;
import io.xpipe.beacon.BeaconClientException;
import io.xpipe.beacon.api.TerminalPrepareExchange;
import io.xpipe.beacon.api.TerminalRegisterExchange;

public class TerminalRegisterExchangeImpl extends TerminalRegisterExchange {

    @Override
    public Object handle(HttpExchange exchange, Request msg) throws BeaconClientException {
        TerminalView.get().open(msg.getRequest(), msg.getPid());
        TerminalLauncherManager.registerPid(msg.getRequest(), msg.getPid());
        return Response.builder()
                .build();
    }

    @Override
    public boolean requiresEnabledApi() {
        return false;
    }
}
