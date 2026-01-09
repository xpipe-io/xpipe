package io.xpipe.app.beacon.impl;

import io.xpipe.app.beacon.AppBeaconServer;
import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.beacon.BeaconClientException;
import io.xpipe.beacon.api.ShellStartExchange;
import io.xpipe.core.JacksonMapper;

import com.sun.net.httpserver.HttpExchange;
import lombok.SneakyThrows;

public class ShellStartExchangeImpl extends ShellStartExchange {

    @Override
    @SneakyThrows
    public Object handle(HttpExchange exchange, Request msg) {
        var e = DataStorage.get()
                .getStoreEntryIfPresent(msg.getConnection())
                .orElseThrow(() -> new BeaconClientException("Unknown connection"));
        if (!(e.getStore() instanceof ShellStore)) {
            throw new BeaconClientException("Not a shell connection");
        }

        var shellSession =
                AppBeaconServer.get().getCache().getOrStart(new DataStoreEntryRef<ShellStore>(e));
        var control = shellSession.getControl();
        var ttyState = JacksonMapper.getDefault().valueToTree(control.getTtyState()).asText();
        return Response.builder()
                .shellDialect(control.getShellDialect().getId())
                .osType(control.getOsType())
                .osName(control.getOsName())
                .temp(control.getSystemTemporaryDirectory())
                .ttyState(ttyState)
                .build();
    }
}
