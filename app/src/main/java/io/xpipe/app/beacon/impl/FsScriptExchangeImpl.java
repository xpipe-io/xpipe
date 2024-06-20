package io.xpipe.app.beacon.impl;

import com.sun.net.httpserver.HttpExchange;
import io.xpipe.app.beacon.AppBeaconServer;
import io.xpipe.app.beacon.BlobManager;
import io.xpipe.app.util.ScriptHelper;
import io.xpipe.beacon.api.FsScriptExchange;
import lombok.SneakyThrows;

import java.nio.charset.StandardCharsets;

public class FsScriptExchangeImpl extends FsScriptExchange {

    @Override
    @SneakyThrows
    public Object handle(HttpExchange exchange, Request msg) {
        var shell = AppBeaconServer.get().getCache().getShellSession(msg.getConnection());
        String data;
        try (var in = BlobManager.get().getBlob(msg.getBlob())) {
            data = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        var file = ScriptHelper.getExecScriptFile(shell.getControl());
        shell.getControl().getShellDialect().createScriptTextFileWriteCommand(shell.getControl(), data, file.toString()).execute();
        return Response.builder().path(file).build();
    }
}
