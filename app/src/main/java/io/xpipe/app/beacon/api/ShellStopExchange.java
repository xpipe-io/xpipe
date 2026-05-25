package io.xpipe.app.beacon.api;

import com.sun.net.httpserver.HttpExchange;
import io.xpipe.app.beacon.AppBeaconServer;
import io.xpipe.app.beacon.BeaconInterface;

import lombok.Builder;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

public class ShellStopExchange extends BeaconInterface<ShellStopExchange.Request> {

    @Override
    public String getPath() {
        return "/shell/stop";
    }

    @Override
    @SneakyThrows
    public Object handle(HttpExchange exchange, Request msg) {
        var e = AppBeaconServer.get().getCache().getShellSession(msg.getConnection());
        e.getControl().close();
        AppBeaconServer.get().getCache().getShellSessions().remove(e);
        return Response.builder().build();
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request {
        @NonNull
        UUID connection;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response {}
}
