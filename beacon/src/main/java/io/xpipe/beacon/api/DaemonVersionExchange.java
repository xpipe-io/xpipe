package io.xpipe.beacon.api;

import io.xpipe.beacon.BeaconInterface;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

public class DaemonVersionExchange extends BeaconInterface<DaemonVersionExchange.Request> {

    @Override
    public String getPath() {
        return "/daemon/version";
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
        String version;

        @NonNull
        String canonicalVersion;

        @NonNull
        String buildVersion;

        @NonNull
        String jvmVersion;

        @NonNull
        String plan;
    }
}
