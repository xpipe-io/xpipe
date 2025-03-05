package io.xpipe.beacon.api;

import io.xpipe.beacon.BeaconInterface;
import io.xpipe.core.store.FilePath;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

public class ConnectionBrowseExchange extends BeaconInterface<ConnectionBrowseExchange.Request> {

    @Override
    public String getPath() {
        return "/connection/browse";
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request {
        @NonNull
        UUID connection;

        FilePath directory;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response {}
}
