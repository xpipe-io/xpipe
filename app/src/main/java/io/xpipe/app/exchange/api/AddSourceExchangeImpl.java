package io.xpipe.app.exchange.api;

import io.xpipe.app.exchange.MessageExchangeImpl;
import io.xpipe.app.storage.DataSourceEntry;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.beacon.BeaconHandler;
import io.xpipe.beacon.exchange.AddSourceExchange;

import java.util.UUID;

public class AddSourceExchangeImpl extends AddSourceExchange
        implements MessageExchangeImpl<AddSourceExchange.Request, AddSourceExchange.Response> {

    @Override
    public Response handleRequest(BeaconHandler handler, Request msg) throws Exception {

        var noTarget = msg.getTarget() == null;
        var colName = noTarget ? null : msg.getTarget().getCollectionName();
        var entryName =
                noTarget ? UUID.randomUUID().toString() : msg.getTarget().getEntryName();

        var entry = DataSourceEntry.createNew(UUID.randomUUID(), entryName, msg.getSource());
        entry.refresh(true);
        DataStorage.get().add(entry, DataStorage.get().createOrGetCollection(colName));

        return Response.builder().id(DataStorage.get().getId(entry)).build();
    }
}
