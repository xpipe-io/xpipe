package io.xpipe.beacon.message.impl;

import io.xpipe.beacon.message.MessageExchange;
import io.xpipe.beacon.message.RequestMessage;
import io.xpipe.beacon.message.ResponseMessage;
import io.xpipe.core.source.DataSourceId;

public class ReadTableDataExchange implements MessageExchange<ReadTableDataExchange.Request, ReadTableDataExchange.Response> {

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
