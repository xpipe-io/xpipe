package io.xpipe.app.exchange.api;

import io.xpipe.app.exchange.MessageExchangeImpl;
import io.xpipe.beacon.BeaconHandler;
import io.xpipe.beacon.exchange.ForwardExchange;
import io.xpipe.core.source.WriteMode;

public class ForwardExchangeImpl extends ForwardExchange
        implements MessageExchangeImpl<ForwardExchange.Request, ForwardExchange.Response> {

    @Override
    public Response handleRequest(BeaconHandler handler, Request msg) throws Exception {
        var source = getSourceEntry(msg.getSource(), null, false);
        var target = getSourceEntry(msg.getTarget(), null, false);
        try (var con = source.getSource().openReadConnection();
                var outCon =
                        target.getSource().openWriteConnection(msg.isAppend() ? WriteMode.APPEND : WriteMode.REPLACE)) {
            con.init();
            outCon.init();
            con.forward(outCon);
        }
        return ForwardExchange.Response.builder().build();
    }
}
