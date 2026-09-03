package io.xpipe.app.beacon.api;

import io.xpipe.app.beacon.AppBeaconServer;
import io.xpipe.app.beacon.BeaconClientException;
import io.xpipe.app.beacon.BeaconInterface;
import io.xpipe.app.beacon.BlobManager;
import io.xpipe.app.fs.ShellFileSystem;
import io.xpipe.app.util.FilePath;

import com.sun.net.httpserver.HttpExchange;
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

        if (!msg.getPath().isAbsolute()) {
            throw new BeaconClientException("File path " + msg.getPath() + " is not absolute");
        }

        if (!fs.directoryExists(msg.getPath().getParent())) {
            throw new BeaconClientException("Directory " + msg.getPath().getParent() + " does not exist");
        }

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
