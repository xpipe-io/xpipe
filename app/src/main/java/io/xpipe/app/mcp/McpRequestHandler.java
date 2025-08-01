package io.xpipe.app.mcp;

import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStorageQuery;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.beacon.BeaconClientException;

public interface McpRequestHandler {

    default DataStoreEntryRef<ShellStore> getShellStoreRef(String name) throws BeaconClientException {
        var found = DataStorageQuery.queryUserInput(name);
        if (found.isEmpty()) {
            throw new BeaconClientException("No connection found for input " + name);
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

        return e.ref();
    }
}
