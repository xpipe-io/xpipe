package io.xpipe.app.beacon.api;

import com.sun.net.httpserver.HttpExchange;
import io.xpipe.app.beacon.BeaconInterface;

import io.xpipe.app.core.mode.AppOperationMode;
import io.xpipe.app.util.ThreadHelper;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * Requests the daemon to stop.
 */
public class DaemonStopExchange extends BeaconInterface<DaemonStopExchange.Request> {

    @Override
    public String getPath() {
        return "/daemon/stop";
    }

    @Override
    public boolean requiresCompletedStartup() {
        return false;
    }

    @Override
    public Object handle(HttpExchange exchange, Request msg) {
        ThreadHelper.runAsync(() -> {
            ThreadHelper.sleep(1000);
            AppOperationMode.close();
        });
        return Response.builder().success(true).build();
    }

    @Override
    public boolean requiresEnabledApi() {
        return false;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request {}

    @Jacksonized
    @Builder
    @Value
    public static class Response {
        boolean success;
    }
}
