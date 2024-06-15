package io.xpipe.beacon.api;

import io.xpipe.beacon.BeaconInterface;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * Requests the daemon to stop.
 */
public class DaemonStopExchange extends BeaconInterface<DaemonStopExchange.Request> {

    @Override
    public String getPath() {
        return "/daemon/stop";
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request {}

    @Jacksonized
    @Builder
    @Value
    public static class Response {
        boolean success;
    }
}
