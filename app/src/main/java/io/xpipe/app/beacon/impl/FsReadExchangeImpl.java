package io.xpipe.app.beacon.impl;

import com.sun.net.httpserver.HttpExchange;
import io.xpipe.app.beacon.AppBeaconServer;
import io.xpipe.beacon.api.FsReadExchange;
import io.xpipe.core.store.ConnectionFileSystem;
import lombok.SneakyThrows;

public class FsReadExchangeImpl extends FsReadExchange {

    @Override
    @SneakyThrows
    public Object handle(HttpExchange exchange, Request msg) {
        var shell = AppBeaconServer.get().getCache().getShellSession(msg.getConnection());
        var fs = new ConnectionFileSystem(shell.getControl());
        byte[] bytes;
        try (var in = fs.openInput(msg.getPath().toString())) {
            bytes = in.readAllBytes();
        }
        exchange.sendResponseHeaders(200, bytes.length);
        try (var out = exchange.getResponseBody()) {
            out.write(bytes);
        }
        return Response.builder().build();
    }
}
