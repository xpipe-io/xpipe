package io.xpipe.app.exchange;

import io.xpipe.beacon.BeaconHandler;
import io.xpipe.beacon.exchange.ProxyReadConnectionExchange;
import io.xpipe.core.impl.OutputStreamStore;
import io.xpipe.core.source.DataSource;
import io.xpipe.core.source.DataSourceReadConnection;
import io.xpipe.core.source.WriteMode;

public class ProxyReadConnectionExchangeImpl extends ProxyReadConnectionExchange
        implements MessageExchangeImpl<ProxyReadConnectionExchange.Request, ProxyReadConnectionExchange.Response> {

    @Override
    public Response handleRequest(BeaconHandler handler, Request msg) {
        handler.postResponse(() -> {
            var outputSource = DataSource.createInternalDataSource(
                    msg.getSource().getType(), new OutputStreamStore(handler.sendBody()));
            try (DataSourceReadConnection r = msg.getSource().openReadConnection();
                    var w = outputSource.openWriteConnection(WriteMode.REPLACE)) {
                r.init();
                w.init();
                r.forward(w);
            }
        });
        return ProxyReadConnectionExchange.Response.builder().build();
    }
}
