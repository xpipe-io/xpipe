package io.xpipe.app.beacon.api;

import com.sun.net.httpserver.HttpExchange;
import io.xpipe.app.beacon.BeaconClientException;
import io.xpipe.app.beacon.BeaconInterface;

import io.xpipe.app.beacon.BeaconServerException;
import io.xpipe.app.terminal.TerminalLauncherManager;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.nio.file.Path;
import java.util.UUID;

public class TerminalLaunchExchange extends BeaconInterface<TerminalLaunchExchange.Request> {

    @Override
    public String getPath() {
        return "/terminal/launch";
    }

    @Override
    public Object handle(HttpExchange exchange, Request msg) throws BeaconClientException, BeaconServerException {
        var r = TerminalLauncherManager.launchExchange(msg.getRequest());
        return Response.builder().targetFile(r).build();
    }

    @Override
    public boolean requiresEnabledApi() {
        return false;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request {
        @NonNull
        UUID request;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response {
        @NonNull
        Path targetFile;
    }
}
