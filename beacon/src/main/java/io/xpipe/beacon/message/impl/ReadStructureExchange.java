package io.xpipe.beacon.message.impl;

import io.xpipe.beacon.socket.SocketServer;
import io.xpipe.beacon.message.MessageProvider;
import io.xpipe.beacon.message.RequestMessage;
import io.xpipe.beacon.message.ResponseMessage;
import io.xpipe.core.source.DataSourceId;
import io.xpipe.storage.DataSourceStorage;

import java.io.InputStream;
import java.net.Socket;

public class ReadStructureExchange implements MessageProvider<ReadStructureExchange.Request, ReadStructureExchange.Response> {

    @Override
    public String getId() {
        return "readStructure";
    }

    @Override
    public Class<Request> getRequestClass() {
        return Request.class;
    }

    @Override
    public Class<Response> getResponseClass() {
        return Response.class;
    }

    public static record Request(DataSourceId id) implements RequestMessage {

    }

    public static record Response() implements ResponseMessage {

    }
}
