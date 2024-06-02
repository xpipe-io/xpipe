package io.xpipe.beacon.api;

import io.xpipe.beacon.BeaconInterface;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

public class VersionExchange extends BeaconInterface<VersionExchange.Request> {

    @Override
    public String getPath() {
        return "/version";
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request {
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response {

        String version;
        String buildVersion;
        String jvmVersion;
    }
}
