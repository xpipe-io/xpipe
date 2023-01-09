package io.xpipe.beacon.exchange.cli;

import io.xpipe.beacon.RequestMessage;
import io.xpipe.beacon.ResponseMessage;
import io.xpipe.beacon.exchange.MessageExchange;
import io.xpipe.core.util.XPipeDaemonMode;
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
        XPipeDaemonMode mode;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response implements ResponseMessage {
        @NonNull
        XPipeDaemonMode usedMode;
    }
}
