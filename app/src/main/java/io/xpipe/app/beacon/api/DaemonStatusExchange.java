package io.xpipe.app.beacon.api;

import com.sun.net.httpserver.HttpExchange;
import io.xpipe.app.beacon.BeaconInterface;

import io.xpipe.app.core.mode.AppOperationMode;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

public class DaemonStatusExchange extends BeaconInterface<DaemonStatusExchange.Request> {

    @Override
    public String getPath() {
        return "/daemon/status";
    }

    @Override
    public boolean requiresCompletedStartup() {
        return false;
    }

    @Override
    public boolean requiresBody() {
        return false;
    }

    @Override
    public Object handle(HttpExchange exchange, Request body) {
        String mode;
        if (AppOperationMode.get() == null) {
            mode = "none";
        } else {
            mode = AppOperationMode.get().getId();
        }

        return Response.builder().mode(mode).build();
    }

    @Override
    public boolean requiresEnabledApi() {
        return false;
    }

    @Value
    @Jacksonized
    @Builder
    public static class Request {}

    @Jacksonized
    @Builder
    @Value
    public static class Response {
        String mode;
    }
}
