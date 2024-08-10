package io.xpipe.beacon.api;

import io.xpipe.beacon.BeaconInterface;
import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellDialect;
import io.xpipe.core.process.ShellTtyState;
import io.xpipe.core.store.FilePath;

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
        ShellDialect shellDialect;

        @NonNull
        OsType osType;

        @NonNull
        String osName;

        @NonNull
        ShellTtyState ttyState;

        @NonNull
        FilePath temp;
    }
}
