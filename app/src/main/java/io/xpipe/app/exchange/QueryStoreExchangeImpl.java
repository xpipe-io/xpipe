package io.xpipe.app.exchange;

import io.xpipe.beacon.BeaconHandler;
import io.xpipe.beacon.exchange.QueryStoreExchange;
import io.xpipe.core.dialog.DialogMapper;

public class QueryStoreExchangeImpl extends QueryStoreExchange
        implements MessageExchangeImpl<QueryStoreExchange.Request, QueryStoreExchange.Response> {

    @Override
    public Response handleRequest(BeaconHandler handler, Request msg) throws Exception {
        var store = getStoreEntryByName(msg.getName(), true);
        var summary = "";
        var dialog = store.getProvider().dialogForStore(store.getStore().asNeeded());
        var config = new DialogMapper(dialog).handle();
        return Response.builder()
                .summary(summary)
                .internalStore(store.getStore())
                .provider(store.getProvider().getId())
                .config(config)
                .build();
    }
}
