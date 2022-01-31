package io.xpipe.beacon.exchange;

import io.xpipe.beacon.message.RequestMessage;
import io.xpipe.beacon.message.ResponseMessage;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

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

    @Jacksonized
    @Builder
    @Value
    public static class Request implements RequestMessage {
        @NonNull
        String modeId;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response implements ResponseMessage {

    }
}
