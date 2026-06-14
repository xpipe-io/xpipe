package io.xpipe.app.beacon.api;

import com.sun.net.httpserver.HttpExchange;
import io.xpipe.app.beacon.AppBeaconServer;
import io.xpipe.app.beacon.BeaconClientException;
import io.xpipe.app.beacon.BeaconInterface;
import io.xpipe.app.beacon.BlobManager;
import io.xpipe.app.ext.ConnectionFileSystem;
import io.xpipe.app.util.FilePath;

import io.xpipe.app.util.FixedSizeInputStream;
import lombok.Builder;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.io.BufferedInputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.UUID;

public class FsReadExchange extends BeaconInterface<FsReadExchange.Request> {

    @Override
    public String getPath() {
        return "/fs/read";
    }

    @Override
    @SneakyThrows
    public Object handle(HttpExchange exchange, Request msg) {
        var shell = AppBeaconServer.get().getCache().getShellSession(msg.getStore());
        var fs = new ConnectionFileSystem(shell.getControl());

        if (!fs.fileExists(msg.getPath())) {
            throw new BeaconClientException("File does not exist");
        }

        var size = fs.getFileSize(msg.getPath());
        if (size > 100_000_000) {
            var file = BlobManager.get().newBlobFile();
            try (var in = fs.openInput(msg.getPath())) {
                var fixedIn = new FixedSizeInputStream(new BufferedInputStream(in), size);
                try (var fileOut = Files.newOutputStream(file)) {
                    fixedIn.transferTo(fileOut);
                }
                in.transferTo(OutputStream.nullOutputStream());
            }

            exchange.sendResponseHeaders(200, size);
            try (var fileIn = Files.newInputStream(file);
                    var out = exchange.getResponseBody()) {
                fileIn.transferTo(out);
            }
        } else {
            byte[] bytes;
            try (var in = fs.openInput(msg.getPath())) {
                var fixedIn = new FixedSizeInputStream(new BufferedInputStream(in), size);
                bytes = fixedIn.readAllBytes();
                in.transferTo(OutputStream.nullOutputStream());
            }
            exchange.sendResponseHeaders(200, bytes.length);
            try (var out = exchange.getResponseBody()) {
                out.write(bytes);
            }
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
        FilePath path;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response {}
}
