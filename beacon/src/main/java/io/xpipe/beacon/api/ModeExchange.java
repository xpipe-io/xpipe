package io.xpipe.beacon.api;

import io.xpipe.beacon.BeaconInterface;
import io.xpipe.core.util.XPipeDaemonMode;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

public class ModeExchange extends BeaconInterface<ModeExchange.Request> {

    @Override
    public String getPath() {
        return "/mode";
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
    public static class Response  {
        @NonNull
        XPipeDaemonMode usedMode;
    }
}
