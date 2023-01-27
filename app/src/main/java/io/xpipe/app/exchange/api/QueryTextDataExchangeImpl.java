package io.xpipe.app.exchange.api;

import io.xpipe.app.exchange.MessageExchangeImpl;
import io.xpipe.beacon.BeaconHandler;
import io.xpipe.beacon.exchange.api.QueryTextDataExchange;
import io.xpipe.core.source.DataSourceType;
import io.xpipe.core.source.TextReadConnection;

import java.nio.charset.StandardCharsets;

public class QueryTextDataExchangeImpl extends QueryTextDataExchange
        implements MessageExchangeImpl<QueryTextDataExchange.Request, QueryTextDataExchange.Response> {

    @Override
    public Response handleRequest(BeaconHandler handler, Request msg) throws Exception {
        var ds = getSourceEntry(msg.getRef(), DataSourceType.TEXT, false);
        handler.postResponse(() -> {
            try (var out = handler.sendBody();
                    var con = ((TextReadConnection) ds.getSource().openReadConnection())) {
                con.init();
                try (var stream = con.lines().limit(msg.getMaxLines() == -1 ? Long.MAX_VALUE : msg.getMaxLines())) {
                    for (var s : stream.toList()) {
                        out.write(s.getBytes(StandardCharsets.UTF_8));
                        out.write("\n".getBytes(StandardCharsets.UTF_8));
                    }
                }
            }
        });
        return QueryTextDataExchange.Response.builder().build();
    }
}
