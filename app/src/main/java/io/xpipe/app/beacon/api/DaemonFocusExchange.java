package io.xpipe.app.beacon.api;

import com.sun.net.httpserver.HttpExchange;
import io.xpipe.app.beacon.BeaconInterface;

import io.xpipe.app.core.mode.AppOperationMode;
import io.xpipe.app.core.window.AppMainWindow;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

public class DaemonFocusExchange extends BeaconInterface<DaemonFocusExchange.Request> {

    @Override
    public String getPath() {
        return "/daemon/focus";
    }

    @Override
    public boolean requiresBody() {
        return false;
    }

    @Override
    public Object handle(HttpExchange exchange, Request msg) throws Throwable {
        if (AppOperationMode.isInStartup()) {
            return Response.builder().build();
        }

        if (AppOperationMode.GUI.isSupported()) {
            AppOperationMode.switchToSyncOrThrow(AppOperationMode.GUI);
        }

        var w = AppMainWindow.get();
        if (w != null) {
            w.focus();
        }
        return Response.builder().build();
    }

    @Override
    public boolean requiresEnabledApi() {
        return false;
    }

    @Override
    public boolean requiresCompletedStartup() {
        return false;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request {}

    @Jacksonized
    @Builder
    @Value
    public static class Response {}
}
