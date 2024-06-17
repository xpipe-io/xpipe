package io.xpipe.app.beacon.impl;

import com.sun.net.httpserver.HttpExchange;
import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.beacon.api.DaemonStopExchange;

public class DaemonStopExchangeImpl extends DaemonStopExchange {

    @Override
    public boolean requiresCompletedStartup() {
        return false;
    }

    @Override
    public Object handle(HttpExchange exchange, Request msg) {
        ThreadHelper.runAsync(() -> {
            ThreadHelper.sleep(1000);
            OperationMode.close();
        });
        return Response.builder().success(true).build();
    }
}
