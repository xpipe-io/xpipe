package io.xpipe.app.exchange.cli;

import io.xpipe.app.exchange.DialogExchangeImpl;
import io.xpipe.app.exchange.MessageExchangeImpl;
import io.xpipe.beacon.BeaconHandler;
import io.xpipe.beacon.exchange.cli.EditStoreExchange;
import io.xpipe.core.store.DataStore;

public class EditStoreExchangeImpl extends EditStoreExchange
        implements MessageExchangeImpl<EditStoreExchange.Request, EditStoreExchange.Response> {

    @Override
    public Response handleRequest(BeaconHandler handler, Request msg) throws Exception {
        var s = getStoreEntryByName(msg.getName(), false);
        var dialog = s.getProvider().dialogForStore(s.getStore());
        var reference = DialogExchangeImpl.add(dialog, (DataStore newStore) -> {
            s.setStore(newStore);
        });
        return Response.builder().dialog(reference).build();
    }
}
