package io.xpipe.beacon.api;

import io.xpipe.beacon.BeaconInterface;
import io.xpipe.core.FilePath;
import io.xpipe.core.OsType;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

public class ShellStartExchange extends BeaconInterface<ShellStartExchange.Request> {

    @Override
    public String getPath() {
        return "/shell/start";
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request {
        @NonNull
        UUID connection;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response {
        @NonNull
        String shellDialect;

        @NonNull
        OsType.Any osType;

        @NonNull
        String osName;

        @NonNull
        String ttyState;

        @NonNull
        FilePath temp;
    }
}
