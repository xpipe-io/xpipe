package io.xpipe.app.beacon.impl;

import com.sun.net.httpserver.HttpExchange;
import io.xpipe.app.beacon.AppBeaconServer;
import io.xpipe.beacon.api.FsWriteExchange;
import io.xpipe.core.store.ConnectionFileSystem;
import lombok.SneakyThrows;

public class FsWriteExchangeImpl extends FsWriteExchange {

    @Override
    @SneakyThrows
    public Object handle(HttpExchange exchange, Request msg) {
        var shell = AppBeaconServer.get().getCache().getShellSession(msg.getConnection());
        var data = AppBeaconServer.get().getCache().getBlob(msg.getBlob());
        var fs = new ConnectionFileSystem(shell.getControl());
        try (var os = fs.openOutput(msg.getPath().toString(), data.length)) {
            os.write(data);
        }
        return Response.builder().build();
    }
}
