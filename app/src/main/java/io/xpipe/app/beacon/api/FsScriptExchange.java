package io.xpipe.app.beacon.api;

import com.sun.net.httpserver.HttpExchange;
import io.xpipe.app.beacon.AppBeaconServer;
import io.xpipe.app.beacon.BeaconInterface;
import io.xpipe.app.beacon.BlobManager;
import io.xpipe.app.process.ScriptHelper;
import io.xpipe.app.util.FilePath;

import lombok.Builder;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class FsScriptExchange extends BeaconInterface<FsScriptExchange.Request> {

    @Override
    public String getPath() {
        return "/fs/script";
    }

    @Override
    @SneakyThrows
    public Object handle(HttpExchange exchange, Request msg) {
        var shell = AppBeaconServer.get().getCache().getShellSession(msg.getStore());
        String data;
        try (var in = BlobManager.get().getBlob(msg.getBlob())) {
            data = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        data = shell.getControl().getShellDialect().prepareScriptContent(shell.getControl(), data);
        var file = ScriptHelper.createExecScript(shell.getControl(), data);
        return Response.builder().path(file).build();
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request {
        @NonNull
        UUID store;

        @NonNull
        UUID blob;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response {
        @NonNull
        FilePath path;
    }
}
