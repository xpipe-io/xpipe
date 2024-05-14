package io.xpipe.app.beacon.impl;

import com.sun.net.httpserver.HttpExchange;
import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.beacon.BeaconClientException;
import io.xpipe.beacon.BeaconServerException;
import io.xpipe.beacon.api.StopExchange;

import java.io.IOException;

public class StopExchangeImpl extends StopExchange {

    @Override
    public Object handle(HttpExchange exchange, Request msg) throws IOException, BeaconClientException, BeaconServerException {
        ThreadHelper.runAsync(() -> {
            ThreadHelper.sleep(1000);
            OperationMode.close();
        });
        return Response.builder().success(true).build();
    }
}
