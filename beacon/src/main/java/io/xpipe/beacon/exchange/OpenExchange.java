package io.xpipe.beacon.exchange;

import io.xpipe.beacon.RequestMessage;
import io.xpipe.beacon.ResponseMessage;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

public class OpenExchange implements MessageExchange {

    @Override
    public String getId() {
        return "open";
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request implements RequestMessage {
        @NonNull List<String> arguments;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response implements ResponseMessage {}
}
