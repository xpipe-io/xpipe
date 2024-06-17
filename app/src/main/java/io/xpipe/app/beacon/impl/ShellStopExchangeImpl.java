package io.xpipe.app.beacon.impl;

import com.sun.net.httpserver.HttpExchange;
import io.xpipe.app.beacon.AppBeaconServer;
import io.xpipe.beacon.api.ShellStopExchange;
import lombok.SneakyThrows;

public class ShellStopExchangeImpl extends ShellStopExchange {

    @Override
    @SneakyThrows
    public Object handle(HttpExchange exchange, Request msg) {
        var e = AppBeaconServer.get().getCache().getShellSession(msg.getConnection());
        e.getControl().close();
        AppBeaconServer.get().getCache().getShellSessions().remove(e);
        return Response.builder().build();
    }
}
