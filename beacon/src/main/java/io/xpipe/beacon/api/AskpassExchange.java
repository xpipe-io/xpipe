package io.xpipe.beacon.api;

import io.xpipe.beacon.BeaconInterface;
import io.xpipe.core.util.SecretValue;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

public class AskpassExchange extends BeaconInterface<AskpassExchange.Request> {

    @Override
    public String getPath() {
        return "/askpass";
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request {
        UUID secretId;

        UUID request;

        String prompt;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response {
        SecretValue value;
    }
}
