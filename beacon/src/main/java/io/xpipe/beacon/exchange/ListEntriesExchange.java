package io.xpipe.beacon.exchange;

import io.xpipe.beacon.exchange.data.EntryListEntry;
import io.xpipe.beacon.message.RequestMessage;
import io.xpipe.beacon.message.ResponseMessage;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

public class ListEntriesExchange implements MessageExchange<ListEntriesExchange.Request, ListEntriesExchange.Response> {

    @Override
    public String getId() {
        return "listEntries";
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
        String collection;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response implements ResponseMessage {
        List<EntryListEntry> entries;
    }
}
