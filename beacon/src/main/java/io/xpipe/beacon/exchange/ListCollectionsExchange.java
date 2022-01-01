package io.xpipe.beacon.exchange;

import io.xpipe.beacon.message.RequestMessage;
import io.xpipe.beacon.message.ResponseMessage;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.List;

public class ListCollectionsExchange implements MessageExchange<ListCollectionsExchange.Request, ListCollectionsExchange.Response> {

    @Override
    public String getId() {
        return "listCollections";
    }

    @Override
    public Class<Request> getRequestClass() {
        return Request.class;
    }

    @Override
    public Class<Response> getResponseClass() {
        return Response.class;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request implements RequestMessage {

    }

    public static record Entry(String name, int size, Instant lastUsed) {

    }

    @Jacksonized
    @Builder
    @Value
    public static class Response implements ResponseMessage {
        List<Entry> entries;
    }
}
