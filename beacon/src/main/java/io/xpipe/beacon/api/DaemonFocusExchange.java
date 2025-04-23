package io.xpipe.beacon.api;

import io.xpipe.beacon.BeaconInterface;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

public class DaemonFocusExchange extends BeaconInterface<DaemonFocusExchange.Request> {

    @Override
    public String getPath() {
        return "/daemon/focus";
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request {}

    @Jacksonized
    @Builder
    @Value
    public static class Response {}
}
