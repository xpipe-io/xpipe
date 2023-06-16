package io.xpipe.app.exchange.cli;

import io.xpipe.app.exchange.MessageExchangeImpl;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.beacon.BeaconHandler;
import io.xpipe.beacon.exchange.cli.ListCollectionsExchange;
import io.xpipe.beacon.exchange.data.CollectionListEntry;

public class ListCollectionsExchangeImpl extends ListCollectionsExchange
        implements MessageExchangeImpl<ListCollectionsExchange.Request, ListCollectionsExchange.Response> {

    @Override
    public Response handleRequest(BeaconHandler handler, Request msg) {
        DataStorage s = DataStorage.get();
        var e = s.getSourceCollections().stream()
                .map(col -> CollectionListEntry.builder()
                        .name(col.getName())
                        .size(col.getEntries().size())
                        .lastUsed(col.getLastAccess())
                        .build())
                .toList();
        return Response.builder().entries(e).build();
    }
}
