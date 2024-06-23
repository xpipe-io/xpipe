package io.xpipe.beacon.api;

import io.xpipe.beacon.BeaconInterface;
import io.xpipe.core.store.FilePath;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

public class FsWriteExchange extends BeaconInterface<FsWriteExchange.Request> {

    @Override
    public String getPath() {
        return "/fs/write";
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request {
        @NonNull
        UUID connection;

        @NonNull
        UUID blob;

        @NonNull
        FilePath path;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response {}
}
