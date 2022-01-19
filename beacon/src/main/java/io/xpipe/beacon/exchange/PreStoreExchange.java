package io.xpipe.beacon.exchange;

import io.xpipe.beacon.message.RequestMessage;
import io.xpipe.beacon.message.ResponseMessage;
import io.xpipe.core.store.LocalFileDataStore;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.nio.file.Path;

public class PreStoreExchange implements MessageExchange<PreStoreExchange.Request, PreStoreExchange.Response> {

    @Override
    public String getId() {
        return "preStore";
    }

    @Override
    public Class<PreStoreExchange.Request> getRequestClass() {
        return PreStoreExchange.Request.class;
    }

    @Override
    public Class<PreStoreExchange.Response> getResponseClass() {
        return PreStoreExchange.Response.class;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request implements RequestMessage {
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response implements ResponseMessage {
        LocalFileDataStore store;
    }
}
