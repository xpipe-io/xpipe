package io.xpipe.beacon.api;

import io.xpipe.beacon.BeaconInterface;
import io.xpipe.core.XPipeDaemonMode;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

public class DaemonModeExchange extends BeaconInterface<DaemonModeExchange.Request> {

    @Override
    public String getPath() {
        return "/daemon/mode";
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request {
        @NonNull
        XPipeDaemonMode mode;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response {
        @NonNull
        XPipeDaemonMode usedMode;
    }
}
