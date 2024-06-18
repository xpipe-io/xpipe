package io.xpipe.app.beacon.impl;

import com.sun.net.httpserver.HttpExchange;
import io.xpipe.app.beacon.AppBeaconServer;
import io.xpipe.app.util.ScriptHelper;
import io.xpipe.beacon.api.FsScriptExchange;
import lombok.SneakyThrows;

import java.nio.charset.StandardCharsets;

public class FsScriptExchangeImpl extends FsScriptExchange {

    @Override
    @SneakyThrows
    public Object handle(HttpExchange exchange, Request msg) {
        var shell = AppBeaconServer.get().getCache().getShellSession(msg.getConnection());
        var data = new String(AppBeaconServer.get().getCache().getBlob(msg.getBlob()), StandardCharsets.UTF_8);
        var file = ScriptHelper.getExecScriptFile(shell.getControl());
        shell.getControl().getShellDialect().createScriptTextFileWriteCommand(shell.getControl(), data, file.toString()).execute();
        return Response.builder().path(file).build();
    }
}
