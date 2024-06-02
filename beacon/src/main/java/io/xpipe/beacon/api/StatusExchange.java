package io.xpipe.beacon.api;

import io.xpipe.beacon.BeaconInterface;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

public class StatusExchange extends BeaconInterface<StatusExchange.Request> {

    @Override
    public String getPath() {
        return "/status";
    }

    @Value
    @Jacksonized
    @Builder
    public static class Request {
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response {
        String mode;
    }
}
