package io.xpipe.app.beacon.impl;

import com.sun.net.httpserver.HttpExchange;
import io.xpipe.app.beacon.AppBeaconServer;
import io.xpipe.beacon.api.FsBlobExchange;
import lombok.SneakyThrows;

import java.util.UUID;

public class FsBlobExchangeImpl extends FsBlobExchange {

    @Override
    @SneakyThrows
    public Object handle(HttpExchange exchange, Request msg) {
        var id = UUID.randomUUID();
        AppBeaconServer.get().getCache().getSavedBlobs().put(id, msg.getPayload());
        return Response.builder().blob(id).build();
    }
}
