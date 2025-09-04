package io.xpipe.app.beacon.impl;

import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.core.window.AppMainWindow;
import io.xpipe.beacon.api.DaemonFocusExchange;

import com.sun.net.httpserver.HttpExchange;

public class DaemonFocusExchangeImpl extends DaemonFocusExchange {

    @Override
    public Object handle(HttpExchange exchange, Request msg) {
        OperationMode.switchUp(OperationMode.GUI);
        var w = AppMainWindow.get();
        if (w != null) {
            w.focus();
        }
        return Response.builder().build();
    }

    @Override
    public boolean requiresEnabledApi() {
        return false;
    }
}
