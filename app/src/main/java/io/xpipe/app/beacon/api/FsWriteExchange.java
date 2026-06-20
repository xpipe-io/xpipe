package io.xpipe.app.beacon.api;

import com.sun.net.httpserver.HttpExchange;
import io.xpipe.app.beacon.AppBeaconServer;
import io.xpipe.app.beacon.BeaconInterface;
import io.xpipe.app.beacon.BlobManager;
import io.xpipe.app.ext.ShellFileSystem;
import io.xpipe.app.util.FilePath;

import lombok.Builder;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

public class FsWriteExchange extends BeaconInterface<FsWriteExchange.Request> {

    @Override
    public String getPath() {
        return "/fs/write";
    }

    @Override
    @SneakyThrows
    public Object handle(HttpExchange exchange, Request msg) {
        var shell = AppBeaconServer.get().getCache().getShellSession(msg.getStore());
        var fs = new ShellFileSystem(shell.getControl());
        try (var in = BlobManager.get().getBlob(msg.getBlob());
             var os = fs.openOutput(msg.getPath(), BlobManager.get().getSize(msg.getBlob()))) {
            in.transferTo(os);
        }
        return Response.builder().build();
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request {
        @NonNull
        UUID store;

        @NonNull
        UUID blob;

        @NonNull
        FilePath path;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response {}
}
