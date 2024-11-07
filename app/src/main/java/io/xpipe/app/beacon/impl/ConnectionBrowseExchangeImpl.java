package io.xpipe.app.beacon.impl;

import io.xpipe.app.browser.BrowserFullSessionModel;
import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.beacon.BeaconClientException;
import io.xpipe.beacon.api.ConnectionBrowseExchange;
import io.xpipe.core.store.FileSystemStore;

import com.sun.net.httpserver.HttpExchange;

public class ConnectionBrowseExchangeImpl extends ConnectionBrowseExchange {

    @Override
    public Object handle(HttpExchange exchange, Request msg) throws Exception {
        var e = DataStorage.get()
                .getStoreEntryIfPresent(msg.getConnection())
                .orElseThrow(() -> new BeaconClientException("Unknown connection: " + msg.getConnection()));
        if (!(e.getStore() instanceof FileSystemStore)) {
            throw new BeaconClientException("Not a file system connection");
        }
        BrowserFullSessionModel.DEFAULT.openFileSystemSync(
                e.ref(), msg.getDirectory() != null ? ignored -> msg.getDirectory() : null, null, true);
        AppLayoutModel.get().selectBrowser();
        return Response.builder().build();
    }
}
