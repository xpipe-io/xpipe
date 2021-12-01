package io.xpipe.beacon.message.impl;

import io.xpipe.beacon.socket.SocketServer;
import io.xpipe.beacon.message.MessageProvider;
import io.xpipe.beacon.message.RequestMessage;
import io.xpipe.beacon.message.ResponseMessage;
import io.xpipe.core.data.type.DataType;
import io.xpipe.core.source.DataSourceId;
import io.xpipe.storage.DataSourceStorage;

import java.io.InputStream;
import java.net.Socket;

public class ReadTableInfoExchange implements MessageProvider<ReadTableInfoExchange.Request, ReadTableInfoExchange.Response> {

    @Override
    public String getId() {
        return "readTableInfo";
    }

    @Override
    public Class<ReadTableInfoExchange.Request> getRequestClass() {
        return ReadTableInfoExchange.Request.class;
    }

    @Override
    public Class<ReadTableInfoExchange.Response> getResponseClass() {
        return ReadTableInfoExchange.Response.class;
    }

    public static record Request(DataSourceId sourceId) implements RequestMessage {

    }

    public static record Response(DataSourceId sourceId, DataType dataType, int rowCount) implements ResponseMessage {

    }
}
