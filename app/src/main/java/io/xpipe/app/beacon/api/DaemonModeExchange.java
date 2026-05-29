package io.xpipe.app.beacon.api;

import com.sun.net.httpserver.HttpExchange;
import io.xpipe.app.beacon.BeaconClientException;
import io.xpipe.app.beacon.BeaconInterface;
import io.xpipe.app.core.mode.AppOperationMode;
import io.xpipe.app.util.XPipeDaemonMode;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

public class DaemonModeExchange extends BeaconInterface<DaemonModeExchange.Request> {

    @Override
    public String getPath() {
        return "/daemon/mode";
    }

    @Override
    public Object handle(HttpExchange exchange, Request msg) throws BeaconClientException {
        var mode = AppOperationMode.map(msg.getMode());
        if (!mode.isSupported()) {
            throw new BeaconClientException("Unsupported mode: " + msg.getMode().getDisplayName()
                    + ". Supported: "
                    + String.join(
                            ", ",
                            AppOperationMode.getAll().stream()
                                    .filter(AppOperationMode::isSupported)
                                    .map(AppOperationMode::getId)
                                    .toList()));
        }

        AppOperationMode.switchToSyncIfPossible(mode);
        return Response.builder().usedMode(msg.getMode()).build();
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
        XPipeDaemonMode mode;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response {
        @NonNull
        XPipeDaemonMode usedMode;
    }
}
