package io.xpipe.app.beacon.impl;

import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.beacon.BeaconClientException;
import io.xpipe.beacon.api.TerminalPrepareExchange;

import com.sun.net.httpserver.HttpExchange;

public class TerminalPrepareExchangeImpl extends TerminalPrepareExchange {

    @Override
    public Object handle(HttpExchange exchange, Request msg) throws BeaconClientException {
        var term = AppPrefs.get().terminalType().getValue();
        var unicode = term.supportsUnicode();
        var escapes = term.supportsEscapes();
        return Response.builder()
                .supportsUnicode(unicode)
                .supportsEscapeSequences(escapes)
                .build();
    }

    @Override
    public boolean requiresEnabledApi() {
        return false;
    }
}
