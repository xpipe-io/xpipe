package io.xpipe.beacon.exchange;

import io.xpipe.beacon.message.RequestMessage;
import io.xpipe.beacon.message.ResponseMessage;
import io.xpipe.core.store.StreamDataStore;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * Stores a stream of data in a storage.
 */
public class StoreStreamExchange implements MessageExchange<StoreStreamExchange.Request, StoreStreamExchange.Response> {

    @Override
    public String getId() {
        return "storeStream";
    }

    @Override
    public Class<StoreStreamExchange.Request> getRequestClass() {
        return StoreStreamExchange.Request.class;
    }

    @Override
    public Class<StoreStreamExchange.Response> getResponseClass() {
        return StoreStreamExchange.Response.class;
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
        StreamDataStore store;
    }
}
