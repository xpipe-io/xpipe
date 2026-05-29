package io.xpipe.app.beacon.api;

import com.sun.net.httpserver.HttpExchange;
import io.xpipe.app.beacon.BeaconInterface;

import io.xpipe.app.beacon.BeaconServerException;
import io.xpipe.app.terminal.TerminalLauncherManager;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

public class TerminalWaitExchange extends BeaconInterface<TerminalWaitExchange.Request> {

    @Override
    public String getPath() {
        return "/terminal/wait";
    }

    @Override
    public Object handle(HttpExchange exchange, Request msg) throws BeaconServerException {
        TerminalLauncherManager.waitExchange(msg.getRequest());
        return Response.builder().build();
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
    public static class Response {}
}
