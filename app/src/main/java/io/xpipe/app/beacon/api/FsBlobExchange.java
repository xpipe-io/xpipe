package io.xpipe.app.beacon.api;

import com.sun.net.httpserver.HttpExchange;
import io.xpipe.app.beacon.BeaconInterface;

import io.xpipe.app.beacon.BlobManager;
import lombok.Builder;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

public class FsBlobExchange extends BeaconInterface<FsBlobExchange.Request> {

    @Override
    public String getPath() {
        return "/fs/blob";
    }

    @Override
    public boolean readRawRequestBody() {
        return true;
    }

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

    @Jacksonized
    @Builder
    @Value
    public static class Request {}

    @Jacksonized
    @Builder
    @Value
    public static class Response {
        @NonNull
        UUID blob;
    }
}
