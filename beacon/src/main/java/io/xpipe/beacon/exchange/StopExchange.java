package io.xpipe.beacon.exchange;

import io.xpipe.beacon.message.RequestMessage;
import io.xpipe.beacon.message.ResponseMessage;

public abstract class StopExchange implements MessageExchange<StopExchange.Request, StopExchange.Response> {

    @Override
    public String getId() {
        return "stop";
    }

    @Override
    public Class<StopExchange.Request> getRequestClass() {
        return StopExchange.Request.class;
    }

    @Override
    public Class<StopExchange.Response> getResponseClass() {
        return StopExchange.Response.class;
    }

    public static record Request(int timeout, boolean force) implements RequestMessage {

    }

    public static record Response() implements ResponseMessage {

    }
}
