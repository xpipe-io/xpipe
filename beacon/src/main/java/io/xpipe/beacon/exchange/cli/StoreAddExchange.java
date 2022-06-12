package io.xpipe.beacon.exchange.cli;

import io.xpipe.beacon.exchange.MessageExchange;
import io.xpipe.beacon.message.RequestMessage;
import io.xpipe.beacon.message.ResponseMessage;
import io.xpipe.core.config.DialogElement;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

public class StoreAddExchange implements MessageExchange {

    @Override
    public String getId() {
        return "storeAdd";
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request implements RequestMessage {
        @NonNull
        String input;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response implements ResponseMessage {
        UUID dialogKey;
        DialogElement dialogElement;
    }
}
