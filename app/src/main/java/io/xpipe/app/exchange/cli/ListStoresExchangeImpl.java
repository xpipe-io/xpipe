package io.xpipe.app.exchange.cli;

import io.xpipe.app.exchange.MessageExchangeImpl;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.beacon.BeaconHandler;
import io.xpipe.beacon.exchange.cli.ListStoresExchange;
import io.xpipe.beacon.exchange.data.StoreListEntry;

import java.util.Comparator;

public class ListStoresExchangeImpl extends ListStoresExchange
        implements MessageExchangeImpl<ListStoresExchange.Request, ListStoresExchange.Response> {

    @Override
    public Response handleRequest(BeaconHandler handler, Request msg) {
        DataStorage s = DataStorage.get();
        var e = s.getStoreEntries().stream().filter(entry -> !entry.isDisabled()).map(col -> StoreListEntry.builder().id(DataStorage.get().getId(col))
                .type(col.getProvider().getId()).build()).sorted(Comparator.comparing(en -> en.getId().toString())).toList();
        return Response.builder().entries(e).build();
    }
}
