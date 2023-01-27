package io.xpipe.app.exchange.cli;

import io.xpipe.app.exchange.MessageExchangeImpl;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.beacon.BeaconHandler;
import io.xpipe.beacon.ClientException;
import io.xpipe.beacon.exchange.cli.ListEntriesExchange;
import io.xpipe.beacon.exchange.data.EntryListEntry;

import java.util.Optional;

public class ListEntriesExchangeImpl extends ListEntriesExchange
        implements MessageExchangeImpl<ListEntriesExchange.Request, ListEntriesExchange.Response> {

    @Override
    public ListEntriesExchange.Response handleRequest(BeaconHandler handler, Request msg) throws Exception {
        DataStorage s = DataStorage.get();
        var col = s.getCollectionForName(msg.getCollection())
                .or(() -> Optional.ofNullable(
                        msg.getCollection().equalsIgnoreCase("temporary") ? s.getInternalCollection() : null));
        if (col.isEmpty()) {
            throw new ClientException("No collection with name " + msg.getCollection() + " was found");
        }

        var list = col.get().getEntries().stream()
                .map(e -> {
                    return EntryListEntry.builder()
                            .name(e.getName())
                            .type(e.getProvider().getId())
                            .description("")
                            .lastUsed(e.getLastUsed())
                            .build();
                })
                .toList();
        return Response.builder().entries(list).build();
    }
}
