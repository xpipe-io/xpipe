package io.xpipe.beacon.api;

import io.xpipe.beacon.BeaconInterface;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

public class OpenExchange extends BeaconInterface<OpenExchange.Request> {

    @Override
    public String getPath() {
        return "/open";
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
