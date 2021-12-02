package io.xpipe.beacon.message.impl;

import io.xpipe.beacon.message.MessageExchange;
import io.xpipe.beacon.message.RequestMessage;
import io.xpipe.beacon.message.ResponseMessage;

public class ModeExchange implements MessageExchange<ModeExchange.Request, ModeExchange.Response> {

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
