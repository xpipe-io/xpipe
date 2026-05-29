package io.xpipe.app.beacon.api;

import com.sun.net.httpserver.HttpExchange;
import io.xpipe.app.beacon.BeaconClientException;
import io.xpipe.app.beacon.BeaconInterface;

import io.xpipe.app.terminal.TerminalLauncherManager;
import io.xpipe.app.terminal.TerminalView;
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

    @Override
    public Object handle(HttpExchange exchange, Request msg) throws BeaconClientException {
        TerminalView.get().open(msg.getRequest(), msg.getPid());
        TerminalLauncherManager.registerPid(msg.getRequest(), msg.getPid());
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

        long pid;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response {}
}
