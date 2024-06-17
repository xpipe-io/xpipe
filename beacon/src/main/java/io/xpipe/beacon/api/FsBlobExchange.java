package io.xpipe.beacon.api;

import io.xpipe.beacon.BeaconInterface;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

public class FsBlobExchange extends BeaconInterface<FsBlobExchange.Request> {

    @Override
    public String getPath() {
        return "/fs/blob";
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request {
        byte @NonNull [] payload;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response {
        @NonNull
        UUID blob;
    }
}
