package io.xpipe.beacon.api;

import io.xpipe.beacon.BeaconInterface;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

public class DaemonStatusExchange extends BeaconInterface<DaemonStatusExchange.Request> {

    @Override
    public String getPath() {
        return "/daemon/status";
    }

    @Value
    @Jacksonized
    @Builder
    public static class Request {}

    @Jacksonized
    @Builder
    @Value
    public static class Response {
        String mode;
    }
}
