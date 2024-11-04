package io.xpipe.app.beacon.impl;

import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.terminal.TerminalLauncher;
import io.xpipe.beacon.BeaconClientException;
import io.xpipe.beacon.api.ConnectionTerminalExchange;

import com.sun.net.httpserver.HttpExchange;

import java.util.UUID;

public class ConnectionTerminalExchangeImpl extends ConnectionTerminalExchange {

    @Override
    public Object handle(HttpExchange exchange, Request msg) throws Exception {
        var e = DataStorage.get()
                .getStoreEntryIfPresent(msg.getConnection())
                .orElseThrow(() -> new BeaconClientException("Unknown connection: " + msg.getConnection()));
        if (!(e.getStore() instanceof ShellStore shellStore)) {
            throw new BeaconClientException("Not a shell connection");
        }
        var sc = shellStore.getOrStartSession();
        TerminalLauncher.open(e, e.getName(), msg.getDirectory(), sc);
        return Response.builder().build();
    }
}
