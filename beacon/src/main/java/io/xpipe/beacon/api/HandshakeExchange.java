package io.xpipe.beacon.api;

import io.xpipe.beacon.BeaconAuthMethod;
import io.xpipe.beacon.BeaconClientInformation;
import io.xpipe.beacon.BeaconInterface;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

public class HandshakeExchange extends BeaconInterface<HandshakeExchange.Request> {

    @Override
    public String getPath() {
        return "/handshake";
    }

    @Override
    public boolean requiresAuthentication() {
        return false;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request {
        @NonNull
        BeaconAuthMethod auth;
        @NonNull
        BeaconClientInformation client;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response {
        String token;
    }
}
