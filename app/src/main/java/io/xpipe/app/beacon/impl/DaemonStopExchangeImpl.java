package io.xpipe.app.beacon.impl;

import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.beacon.BeaconClientException;
import io.xpipe.beacon.BeaconServerException;
import io.xpipe.beacon.api.DaemonStopExchange;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

public class DaemonStopExchangeImpl extends DaemonStopExchange {

    @Override
    public Object handle(HttpExchange exchange, Request msg) {
        ThreadHelper.runAsync(() -> {
            ThreadHelper.sleep(1000);
            OperationMode.close();
        });
        return Response.builder().success(true).build();
    }
}
