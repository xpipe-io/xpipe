package io.xpipe.app.beacon.impl;

import io.xpipe.app.beacon.AppBeaconServer;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.beacon.BeaconClientException;
import io.xpipe.beacon.BeaconServerException;
import io.xpipe.beacon.api.ShellStopExchange;

import com.sun.net.httpserver.HttpExchange;
import lombok.SneakyThrows;

import java.io.IOException;

public class ShellStopExchangeImpl extends ShellStopExchange {

    @Override
    @SneakyThrows
    public Object handle(HttpExchange exchange, Request msg) {
        var e = DataStorage.get()
                .getStoreEntryIfPresent(msg.getConnection())
                .orElseThrow(() -> new IllegalArgumentException("Unknown connection"));
        var existing = AppBeaconServer.get().getShellSessions().stream()
                .filter(beaconShellSession -> beaconShellSession.getEntry().equals(e))
                .findFirst();
        if (existing.isPresent()) {
            existing.get().getControl().close();
            AppBeaconServer.get().getShellSessions().remove(existing.get());
        }
        return Response.builder().build();
    }
}
