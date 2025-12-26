package io.xpipe.app.beacon.impl;

import io.xpipe.app.core.mode.AppOperationMode;
import io.xpipe.app.core.window.AppMainWindow;
import io.xpipe.beacon.api.DaemonFocusExchange;

import com.sun.net.httpserver.HttpExchange;

public class DaemonFocusExchangeImpl extends DaemonFocusExchange {

    @Override
    public Object handle(HttpExchange exchange, Request msg) throws Throwable {
        if (AppOperationMode.isInStartup()) {
            return Response.builder().build();
        }

        if (AppOperationMode.GUI.isSupported()) {
            AppOperationMode.switchToSyncOrThrow(AppOperationMode.GUI);
        }

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

    @Override
    public boolean requiresCompletedStartup() {
        return false;
    }
}
