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
    public Response handleRequest(BeaconHandler handler, Request msg) throws Exception {
        DataStorage s = DataStorage.get();
        var e = s.getStores().stream()
                .filter(entry -> !entry.isDisabled() && entry.getProvider().shouldShow())
                .sorted(Comparator.comparing(dataStoreEntry -> dataStoreEntry.getLastUsed()))
                .map(col -> StoreListEntry.builder()
                        .name(col.getName())
                        .type(col.getProvider().getId())
                        .build())
                .toList();
        return Response.builder().entries(e).build();
    }
}
