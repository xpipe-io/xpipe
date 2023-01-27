package io.xpipe.app.exchange;

import io.xpipe.beacon.BeaconHandler;
import io.xpipe.beacon.exchange.QueryStoreExchange;
import io.xpipe.core.dialog.DialogMapper;

public class QueryStoreExchangeImpl extends QueryStoreExchange
        implements MessageExchangeImpl<QueryStoreExchange.Request, QueryStoreExchange.Response> {

    @Override
    public Response handleRequest(BeaconHandler handler, Request msg) throws Exception {
        var store = getStoreEntryByName(msg.getName(), true);
        var information = store.getInformation();
        var summary = store.getProvider().toSummaryString(store.getStore(), 100);
        var dialog = store.getProvider().dialogForStore(store.getStore().asNeeded());
        var config = new DialogMapper(dialog).handle();
        return Response.builder()
                .information(information)
                .summary(summary)
                .internalStore(store.getStore())
                .provider(store.getProvider().getId())
                .config(config)
                .build();
    }
}
