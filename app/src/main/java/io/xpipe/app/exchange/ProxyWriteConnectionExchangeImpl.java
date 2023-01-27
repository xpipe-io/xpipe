package io.xpipe.app.exchange;

import io.xpipe.beacon.BeaconHandler;
import io.xpipe.beacon.exchange.ProxyWriteConnectionExchange;
import io.xpipe.core.impl.InputStreamStore;
import io.xpipe.core.source.DataSource;
import io.xpipe.core.source.DataSourceReadConnection;

public class ProxyWriteConnectionExchangeImpl extends ProxyWriteConnectionExchange
        implements MessageExchangeImpl<ProxyWriteConnectionExchange.Request, ProxyWriteConnectionExchange.Response> {

    @Override
    public Response handleRequest(BeaconHandler handler, Request msg) throws Exception {
        var outputSource = msg.getSource();
        var inputSource = DataSource.createInternalDataSource(
                outputSource.getType(), new InputStreamStore(handler.receiveBody()));
        try (DataSourceReadConnection r = inputSource.openReadConnection();
                var w = outputSource.openWriteConnection(msg.getMode())) {
            r.init();
            w.init();
            r.forward(w);
        }
        return Response.builder().build();
    }
}
