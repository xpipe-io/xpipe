package io.xpipe.beacon.api;

import io.xpipe.beacon.BeaconInterface;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

public class TerminalRegisterExchange extends BeaconInterface<TerminalRegisterExchange.Request> {

    @Override
    public String getPath() {
        return "/terminal/register";
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request {
        @NonNull
        UUID request;

        long pid;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response {}
}
