package io.xpipe.app.exchange;

import io.xpipe.app.storage.DataStorage;
import io.xpipe.beacon.BeaconHandler;
import io.xpipe.beacon.exchange.QueryDataSourceExchange;
import io.xpipe.core.dialog.DialogMapper;

public class QueryDataSourceExchangeImpl extends QueryDataSourceExchange
        implements MessageExchangeImpl<QueryDataSourceExchange.Request, QueryDataSourceExchange.Response> {

    @Override
    public Response handleRequest(BeaconHandler handler, Request msg) throws Exception {
        var source = getSourceEntry(msg.getRef(), null, false);
        var id = DataStorage.get().getId(source);
        var information = source.getInformation();
        var dialog = source.getProvider().configDialog(source.getSource().asNeeded(), true);
        var config = new DialogMapper(dialog).handle();
        return QueryDataSourceExchange.Response.builder()
                .id(id)
                .type(source.getDataSourceType())
                .information(information)
                .internalSource(source.getSource())
                .provider(source.getProvider().getId())
                .config(config)
                .build();
    }
}
