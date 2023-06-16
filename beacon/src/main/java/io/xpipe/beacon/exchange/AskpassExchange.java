package io.xpipe.beacon.exchange;

import io.xpipe.beacon.RequestMessage;
import io.xpipe.beacon.ResponseMessage;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

public class AskpassExchange implements MessageExchange {

    @Override
    public String getId() {
        return "askpass";
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request implements RequestMessage {
        @NonNull
        String id;

        @NonNull
        String request;

        String prompt;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response implements ResponseMessage {
        String value;
    }
}
