package io.xpipe.app.beacon.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;
import io.xpipe.app.action.ActionJacksonMapper;
import io.xpipe.app.beacon.BeaconClientException;
import io.xpipe.app.beacon.BeaconInterface;
import io.xpipe.app.core.AppCache;
import io.xpipe.app.core.window.AppDialog;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

public class ActionExchange extends BeaconInterface<ActionExchange.Request> {

    @Override
    public String getPath() {
        return "/action";
    }

    @Override
    public Object handle(HttpExchange exchange, Request msg) throws Exception {
        var action = ActionJacksonMapper.parse(msg.getAction());
        if (action == null) {
            throw new BeaconClientException("Unable to parse action into known schema");
        }

        if (!checkPermission()) {
            return Response.builder().build();
        }

        action.executeSyncImpl(msg.isConfirm());
        return Response.builder().build();
    }

    private boolean checkPermission() {
        var cache = AppCache.getBoolean("externalActionPermitted", false);
        if (cache) {
            return true;
        }

        var r = AppDialog.confirm("externalAction");
        if (r) {
            AppCache.update("externalActionPermitted", true);
        }
        return r;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request {
        @NonNull
        JsonNode action;

        boolean confirm;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response {}
}
