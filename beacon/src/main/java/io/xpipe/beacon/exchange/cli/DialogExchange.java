package io.xpipe.beacon.exchange.cli;

import io.xpipe.beacon.exchange.MessageExchange;
import io.xpipe.beacon.message.RequestMessage;
import io.xpipe.beacon.message.ResponseMessage;
import io.xpipe.core.config.DialogElement;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

public class DialogExchange implements MessageExchange {

    @Override
    public String getId() {
        return "dialog";
    }

    @Override
    public Class<DialogExchange.Request> getRequestClass() {
        return DialogExchange.Request.class;
    }

    @Override
    public Class<DialogExchange.Response> getResponseClass() {
        return DialogExchange.Response.class;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request implements RequestMessage {
        UUID dialogKey;
        String value;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response implements ResponseMessage {
        DialogElement element;
        String errorMsg;
    }
}
