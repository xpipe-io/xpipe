package io.xpipe.beacon.message.impl;

import io.xpipe.beacon.message.MessageExchange;
import io.xpipe.beacon.message.RequestMessage;
import io.xpipe.beacon.message.ResponseMessage;
import io.xpipe.core.data.type.DataType;
import io.xpipe.core.source.DataSourceId;

public class ReadTableInfoExchange implements MessageExchange<ReadTableInfoExchange.Request, ReadTableInfoExchange.Response> {

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
