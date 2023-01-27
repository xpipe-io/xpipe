package io.xpipe.app.exchange;

import io.xpipe.app.storage.DataStorage;
import io.xpipe.beacon.BeaconHandler;
import io.xpipe.beacon.exchange.EditExchange;
import io.xpipe.core.source.DataSource;

public class EditExchangeImpl extends EditExchange
        implements MessageExchangeImpl<EditExchange.Request, EditExchange.Response> {

    @Override
    public Response handleRequest(BeaconHandler handler, Request msg) throws Exception {
        var source = getSourceEntry(msg.getRef(), null, false);
        var provider = source.getProvider();
        var dialog = toCompleteConfig(source.getSource().asNeeded(), provider, true);
        var config = DialogExchangeImpl.add(dialog, (DataSource<?> o) -> {
            source.setSource(o);
        });
        return Response.builder()
                .config(config)
                .id(DataStorage.get().getId(source))
                .build();
    }
}
