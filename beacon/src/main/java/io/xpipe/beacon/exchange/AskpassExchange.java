package io.xpipe.beacon.exchange;

import io.xpipe.beacon.RequestMessage;
import io.xpipe.beacon.ResponseMessage;
import io.xpipe.core.util.SecretValue;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

public class AskpassExchange implements MessageExchange {

    @Override
    public String getId() {
        return "askpass";
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request implements RequestMessage {
        UUID secretId;

        @NonNull
        UUID request;

        String prompt;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response implements ResponseMessage {
        SecretValue value;
    }
}
