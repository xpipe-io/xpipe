package io.xpipe.beacon.exchange;

import io.xpipe.beacon.message.RequestMessage;
import io.xpipe.beacon.message.ResponseMessage;

public abstract class StatusExchange implements MessageExchange<StatusExchange.Request, StatusExchange.Response> {

    @Override
    public String getId() {
        return "status";
    }

    @Override
    public Class<Request> getRequestClass() {
        return Request.class;
    }

    @Override
    public Class<Response> getResponseClass() {
        return Response.class;
    }

    public static record Request() implements RequestMessage {

    }

    public static record Response(String mode) implements ResponseMessage {

    }
}
