package io.xpipe.app.exchange.api;

import io.xpipe.app.exchange.MessageExchangeImpl;
import io.xpipe.beacon.BeaconHandler;
import io.xpipe.beacon.exchange.api.QueryRawDataExchange;
import io.xpipe.core.source.DataSourceType;
import io.xpipe.core.source.RawReadConnection;

public class QueryRawDataExchangeImpl extends QueryRawDataExchange
        implements MessageExchangeImpl<QueryRawDataExchange.Request, QueryRawDataExchange.Response> {

    @Override
    public Response handleRequest(BeaconHandler handler, Request msg) throws Exception {
        var ds = getSourceEntry(msg.getRef(), DataSourceType.RAW, false);
        handler.postResponse(() -> {
            try (var out = handler.sendBody()) {
                try (var con = (RawReadConnection) ds.getSource().openReadConnection()) {
                    con.init();
                    con.forwardBytes(out, msg.getMaxBytes());
                }
            }
        });
        return Response.builder().build();
    }
}
