package io.xpipe.app.beacon.impl;

import io.xpipe.app.beacon.AppBeaconServer;
import io.xpipe.app.beacon.BeaconShellSession;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.beacon.BeaconClientException;
import io.xpipe.beacon.api.ShellStartExchange;
import io.xpipe.core.store.ShellStore;

import com.sun.net.httpserver.HttpExchange;
import lombok.SneakyThrows;

public class ShellStartExchangeImpl extends ShellStartExchange {

    @Override
    @SneakyThrows
    public Object handle(HttpExchange exchange, Request msg) {
        var e = DataStorage.get()
                .getStoreEntryIfPresent(msg.getConnection())
                .orElseThrow(() -> new BeaconClientException("Unknown connection"));
        if (!(e.getStore() instanceof ShellStore s)) {
            throw new BeaconClientException("Not a shell connection");
        }

        var existing = AppBeaconServer.get().getCache().getShellSessions().stream()
                .filter(beaconShellSession -> beaconShellSession.getEntry().equals(e))
                .findFirst();
        var control = (existing.isPresent() ? existing.get().getControl() : s.control());
        control.setNonInteractive();
        control.start();
        if (existing.isEmpty()) {
            AppBeaconServer.get().getCache().getShellSessions().add(new BeaconShellSession(e, control));
        }
        return Response.builder()
                .shellDialect(control.getShellDialect())
                .osType(control.getOsType())
                .osName(control.getOsName())
                .temp(control.getSystemTemporaryDirectory())
                .build();
    }
}
