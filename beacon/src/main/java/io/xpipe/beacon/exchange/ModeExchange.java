package io.xpipe.beacon.exchange;

import io.xpipe.beacon.message.RequestMessage;
import io.xpipe.beacon.message.ResponseMessage;

public abstract class ModeExchange implements MessageExchange<ModeExchange.Request, ModeExchange.Response> {

    @Override
    public String getId() {
        return "mode";
    }

    @Override
    public Class<ModeExchange.Request> getRequestClass() {
        return ModeExchange.Request.class;
    }

    @Override
    public Class<ModeExchange.Response> getResponseClass() {
        return ModeExchange.Response.class;
    }

    public static record Request(String modeId) implements RequestMessage {

    }

    public static record Response() implements ResponseMessage {

    }
}
