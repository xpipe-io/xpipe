package io.xpipe.app.beacon.impl;

import io.xpipe.app.beacon.AppBeaconServer;
import io.xpipe.app.beacon.BlobManager;
import io.xpipe.app.ext.ConnectionFileSystem;
import io.xpipe.app.util.FixedSizeInputStream;
import io.xpipe.beacon.BeaconClientException;
import io.xpipe.beacon.api.FsReadExchange;

import com.sun.net.httpserver.HttpExchange;
import lombok.SneakyThrows;

import java.io.BufferedInputStream;
import java.io.OutputStream;
import java.nio.file.Files;

public class FsReadExchangeImpl extends FsReadExchange {

    @Override
    @SneakyThrows
    public Object handle(HttpExchange exchange, Request msg) {
        var shell = AppBeaconServer.get().getCache().getShellSession(msg.getConnection());
        var fs = new ConnectionFileSystem(shell.getControl());

        if (!fs.fileExists(msg.getPath().toString())) {
            throw new BeaconClientException("File does not exist");
        }

        var size = fs.getFileSize(msg.getPath().toString());
        if (size > 100_000_000) {
            var file = BlobManager.get().newBlobFile();
            try (var in = fs.openInput(msg.getPath().toString())) {
                var fixedIn = new FixedSizeInputStream(new BufferedInputStream(in), size);
                try (var fileOut =
                        Files.newOutputStream(file.resolve(msg.getPath().getFileName()))) {
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
            try (var in = fs.openInput(msg.getPath().toString())) {
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
}
