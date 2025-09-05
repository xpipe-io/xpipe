package io.xpipe.app.beacon.impl;

import io.xpipe.app.core.mode.AppOperationMode;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.beacon.api.DaemonStopExchange;

import com.sun.net.httpserver.HttpExchange;

public class DaemonStopExchangeImpl extends DaemonStopExchange {

    @Override
    public boolean requiresCompletedStartup() {
        return false;
    }

    @Override
    public Object handle(HttpExchange exchange, Request msg) {
        ThreadHelper.runAsync(() -> {
            ThreadHelper.sleep(1000);
            AppOperationMode.close();
        });
        return Response.builder().success(true).build();
    }

    @Override
    public boolean requiresEnabledApi() {
        return false;
    }
}
