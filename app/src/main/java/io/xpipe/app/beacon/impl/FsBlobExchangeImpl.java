package io.xpipe.app.beacon.impl;

import io.xpipe.app.beacon.BlobManager;
import io.xpipe.beacon.api.FsBlobExchange;

import com.sun.net.httpserver.HttpExchange;
import lombok.SneakyThrows;

import java.util.UUID;

public class FsBlobExchangeImpl extends FsBlobExchange {

    @Override
    @SneakyThrows
    public Object handle(HttpExchange exchange, Request msg) {
        var id = UUID.randomUUID();

        var size = exchange.getRequestBody().available();
        if (size > 100_000_000) {
            BlobManager.get().store(id, exchange.getRequestBody());
        } else {
            BlobManager.get().store(id, exchange.getRequestBody().readAllBytes());
        }
        return Response.builder().blob(id).build();
    }
}
