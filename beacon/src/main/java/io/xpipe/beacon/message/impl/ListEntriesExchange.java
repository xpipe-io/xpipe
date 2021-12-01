package io.xpipe.beacon.message.impl;

import io.xpipe.beacon.socket.SocketServer;
import io.xpipe.beacon.message.MessageProvider;
import io.xpipe.beacon.message.RequestMessage;
import io.xpipe.beacon.message.ResponseMessage;
import io.xpipe.storage.DataSourceStorage;

import java.io.InputStream;
import java.net.Socket;
import java.util.List;

public class ListEntriesExchange implements MessageProvider<ListEntriesExchange.Request, ListEntriesExchange.Response> {

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

    private static record Entry(String name, String type, String description, String date, String size) {

    }

    public static record Response(List<Entry> entries) implements ResponseMessage {

    }
}
