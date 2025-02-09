package io.xpipe.app.beacon.impl;

import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.terminal.TerminalLauncherManager;
import io.xpipe.app.terminal.TerminalView;
import io.xpipe.beacon.BeaconClientException;
import io.xpipe.beacon.api.TerminalPrepareExchange;

import com.sun.net.httpserver.HttpExchange;

public class TerminalPrepareExchangeImpl extends TerminalPrepareExchange {

    @Override
    public Object handle(HttpExchange exchange, Request msg) throws BeaconClientException {
        TerminalView.get().open(msg.getRequest(), msg.getPid());
        TerminalLauncherManager.registerPid(msg.getRequest(), msg.getPid());
        var term = AppPrefs.get().terminalType().getValue();
        var unicode = term.supportsUnicode();
        var escapes = term.supportsEscapes();
        var finished = TerminalLauncherManager.isCompletedSuccessfully(msg.getRequest());
        return Response.builder()
                .supportsUnicode(unicode)
                .supportsEscapeSequences(escapes)
                .alreadyFinished(finished)
                .build();
    }

    @Override
    public boolean requiresEnabledApi() {
        return false;
    }
}
