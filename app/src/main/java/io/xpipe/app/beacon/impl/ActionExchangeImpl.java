package io.xpipe.app.beacon.impl;

import io.xpipe.app.action.ActionJacksonMapper;
import io.xpipe.app.core.AppCache;
import io.xpipe.app.core.window.AppDialog;
import io.xpipe.beacon.BeaconClientException;
import io.xpipe.beacon.api.ActionExchange;

import com.sun.net.httpserver.HttpExchange;

public class ActionExchangeImpl extends ActionExchange {

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
}
