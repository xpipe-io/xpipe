package io.xpipe.beacon.api;

import io.xpipe.beacon.BeaconInterface;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

public class SshLaunchExchange extends BeaconInterface<SshLaunchExchange.Request> {

    @Override
    public String getPath() {
        return "/sshLaunch";
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request {
        String storePath;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response {
        @NonNull
        List<String> command;
    }
}
