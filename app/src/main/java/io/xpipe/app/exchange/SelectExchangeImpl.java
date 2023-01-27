package io.xpipe.app.exchange;

import io.xpipe.app.storage.DataStorage;
import io.xpipe.beacon.BeaconHandler;
import io.xpipe.beacon.ClientException;
import io.xpipe.beacon.exchange.cli.SelectExchange;
import io.xpipe.core.source.DataSourceReference;

public class SelectExchangeImpl extends SelectExchange
        implements MessageExchangeImpl<SelectExchange.Request, SelectExchange.Response> {

    @Override
    public Response handleRequest(BeaconHandler handler, Request msg) throws Exception {
        if (msg.getRef().getType() == DataSourceReference.Type.LATEST) {
            throw new ClientException("Can't select latest data source");
        }

        var ds = getSourceEntry(msg.getRef(), null, false);
        DataStorage.get().setLatest(ds);
        return Response.builder().build();
    }
}
