package io.xpipe.beacon.exchange;

import io.xpipe.beacon.message.RequestMessage;
import io.xpipe.beacon.message.ResponseMessage;
import io.xpipe.core.source.DataSourceId;

public class ReadStructureExchange implements MessageExchange<ReadStructureExchange.Request, ReadStructureExchange.Response> {

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
