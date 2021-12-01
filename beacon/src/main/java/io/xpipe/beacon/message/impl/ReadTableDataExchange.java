package io.xpipe.beacon.message.impl;

import io.xpipe.beacon.socket.SocketServer;
import io.xpipe.beacon.message.MessageProvider;
import io.xpipe.beacon.message.RequestMessage;
import io.xpipe.beacon.message.ResponseMessage;
import io.xpipe.core.source.DataSourceId;
import io.xpipe.storage.DataSourceStorage;

import java.io.InputStream;
import java.net.Socket;

public class ReadTableDataExchange implements MessageProvider<ReadTableDataExchange.Request, ReadTableDataExchange.Response> {

    @Override
    public String getId() {
        return "readTable";
    }

    @Override
    public Class<ReadTableDataExchange.Request> getRequestClass() {
        return ReadTableDataExchange.Request.class;
    }

    @Override
    public Class<ReadTableDataExchange.Response> getResponseClass() {
        return ReadTableDataExchange.Response.class;
    }

    public static record Request(DataSourceId sourceId, int maxLines) implements RequestMessage {

    }

    public static record Response() implements ResponseMessage {

    }
}
