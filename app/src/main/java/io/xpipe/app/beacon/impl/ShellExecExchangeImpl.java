package io.xpipe.app.beacon.impl;

import io.xpipe.app.beacon.AppBeaconServer;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.beacon.BeaconClientException;
import io.xpipe.beacon.BeaconServerException;
import io.xpipe.beacon.api.ShellExecExchange;

import com.sun.net.httpserver.HttpExchange;
import lombok.SneakyThrows;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

public class ShellExecExchangeImpl extends ShellExecExchange {

    @Override
    @SneakyThrows
    public Object handle(HttpExchange exchange, Request msg) {
        var e = DataStorage.get()
                .getStoreEntryIfPresent(msg.getConnection())
                .orElseThrow(() -> new IllegalArgumentException("Unknown connection"));
        var existing = AppBeaconServer.get().getShellSessions().stream()
                .filter(beaconShellSession -> beaconShellSession.getEntry().equals(e))
                .findFirst();
        if (existing.isEmpty()) {
            throw new BeaconClientException("No shell session active for connection");
        }

        AtomicReference<String> out = new AtomicReference<>();
        AtomicReference<String> err = new AtomicReference<>();
        long exitCode;
        try (var command = existing.get().getControl().command(msg.getCommand()).start()) {
            command.accumulateStdout(s -> out.set(s));
            command.accumulateStderr(s -> err.set(s));
            exitCode = command.getExitCode();
        }
        return Response.builder()
                .stdout(out.get())
                .stderr(err.get())
                .exitCode(exitCode)
                .build();
    }
}
