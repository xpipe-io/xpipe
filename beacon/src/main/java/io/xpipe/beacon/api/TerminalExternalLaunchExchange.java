package io.xpipe.beacon.api;

import io.xpipe.beacon.BeaconInterface;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

public class TerminalExternalLaunchExchange extends BeaconInterface<TerminalExternalLaunchExchange.Request> {

    @Override
    public String getPath() {
        return "/terminal/externalLaunch";
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request {
        @NonNull
        String connection;

        @NonNull
        List<String> arguments;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response {
        @NonNull
        List<String> command;
    }
}
