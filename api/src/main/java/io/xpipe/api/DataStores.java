package io.xpipe.api;

import io.xpipe.api.connector.XPipeConnection;
import io.xpipe.api.util.QuietDialogHandler;
import io.xpipe.beacon.exchange.cli.StoreAddExchange;
import io.xpipe.core.store.DataStore;

import java.util.Map;

public class DataStores {

    public static void addNamedStore(DataStore store, String name) {
        XPipeConnection.execute(con -> {
            var req = StoreAddExchange.Request.builder()
                    .storeInput(store).name(name).build();
            StoreAddExchange.Response res = con.performSimpleExchange(req);

            new QuietDialogHandler(res.getConfig(), con, Map.of()).handle();
        });
    }
}
