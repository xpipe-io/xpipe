package io.xpipe.beacon.exchange.cli;

import io.xpipe.beacon.exchange.MessageExchange;
import io.xpipe.beacon.message.RequestMessage;
import io.xpipe.beacon.message.ResponseMessage;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

public class ModeExchange implements MessageExchange {

    @Override
    public String getId() {
        return "mode";
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
