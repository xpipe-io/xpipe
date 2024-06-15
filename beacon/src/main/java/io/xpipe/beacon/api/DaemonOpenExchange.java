package io.xpipe.beacon.api;

import io.xpipe.beacon.BeaconInterface;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

public class DaemonOpenExchange extends BeaconInterface<DaemonOpenExchange.Request> {

    @Override
    public String getPath() {
        return "/daemon/open";
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request {
        @NonNull
        List<String> arguments;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response {}
}
