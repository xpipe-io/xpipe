package io.xpipe.beacon.exchange;

import io.xpipe.beacon.message.RequestMessage;
import io.xpipe.beacon.message.ResponseMessage;

import java.util.List;

public abstract class ListEntriesExchange implements MessageExchange<ListEntriesExchange.Request, ListEntriesExchange.Response> {

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

    public static record Request(String collection) implements RequestMessage {

    }

    public static record Entry(String name, String type, String description, String date, String size) {

    }

    public static record Response(List<Entry> entries) implements ResponseMessage {

    }
}
