package io.xpipe.beacon.exchange.cli;

import io.xpipe.beacon.RequestMessage;
import io.xpipe.beacon.ResponseMessage;
import io.xpipe.beacon.XPipeInstance;
import io.xpipe.beacon.exchange.MessageExchange;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

public class InstanceExchange implements MessageExchange {

    @Override
    public String getId() {
        return "instance";
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request implements RequestMessage {

    }

    @Jacksonized
    @Builder
    @Value
    public static class Response implements ResponseMessage {
        @NonNull
        XPipeInstance instance;
    }
}
