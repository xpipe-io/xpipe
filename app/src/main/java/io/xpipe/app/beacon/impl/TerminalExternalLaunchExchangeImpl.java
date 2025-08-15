package io.xpipe.app.beacon.impl;

import io.xpipe.app.core.AppCache;
import io.xpipe.app.core.window.AppDialog;
import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStorageQuery;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.terminal.TerminalLauncherManager;
import io.xpipe.beacon.BeaconClientException;
import io.xpipe.beacon.BeaconServerException;
import io.xpipe.beacon.api.TerminalExternalLaunchExchange;

import com.sun.net.httpserver.HttpExchange;

import java.util.List;

public class TerminalExternalLaunchExchangeImpl extends TerminalExternalLaunchExchange {

    @Override
    public Object handle(HttpExchange exchange, Request msg) throws BeaconClientException, BeaconServerException {
        var found = DataStorageQuery.queryUserInput(msg.getConnection());
        if (found.isEmpty()) {
            throw new BeaconClientException("No connection found for input " + msg.getConnection());
        }

        if (found.size() > 1) {
            throw new BeaconClientException("Multiple connections found: "
                    + found.stream().map(DataStoreEntry::getName).toList());
        }

        var e = found.getFirst();
        var isShell = e.getStore() instanceof ShellStore;
        if (!isShell) {
            throw new BeaconClientException(
                    "Connection " + DataStorage.get().getStorePath(e).toString() + " is not a shell connection");
        }

        if (!checkPermission()) {
            return Response.builder().command(List.of()).build();
        }

        var r = TerminalLauncherManager.externalExchange(e.ref(), msg.getArguments());
        return Response.builder().command(r).build();
    }

    @Override
    public boolean requiresEnabledApi() {
        return false;
    }

    @Override
    public Object getSynchronizationObject() {
        return DataStorage.get();
    }

    private boolean checkPermission() {
        var cache = AppCache.getBoolean("externalLaunchPermitted", false);
        if (cache) {
            return true;
        }

        var r = AppDialog.confirm("externalLaunch");
        if (r) {
            AppCache.update("externalLaunchPermitted", true);
        }
        return r;
    }
}
