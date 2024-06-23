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

    @Override
    public boolean readRawRequestBody() {
        return true;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request {}

    @Jacksonized
    @Builder
    @Value
    public static class Response {
        @NonNull
        UUID blob;
    }
}
