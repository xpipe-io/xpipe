package io.xpipe.beacon.api;

import io.xpipe.beacon.BeaconInterface;
import io.xpipe.core.store.FilePath;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

public class FsReadExchange extends BeaconInterface<FsReadExchange.Request> {

    @Override
    public String getPath() {
        return "/fs/read";
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request {
        @NonNull
        UUID connection;

        @NonNull
        FilePath path;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response {}
}
