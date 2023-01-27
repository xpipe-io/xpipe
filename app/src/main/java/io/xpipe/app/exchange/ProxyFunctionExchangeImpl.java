package io.xpipe.app.exchange;

import io.xpipe.beacon.BeaconHandler;
import io.xpipe.beacon.exchange.ProxyFunctionExchange;

public class ProxyFunctionExchangeImpl extends ProxyFunctionExchange
        implements MessageExchangeImpl<ProxyFunctionExchange.Request, ProxyFunctionExchange.Response> {

    @Override
    public Response handleRequest(BeaconHandler handler, Request msg) throws Exception {
        msg.getFunction().callLocal();
        return ProxyFunctionExchange.Response.builder()
                .function(msg.getFunction())
                .build();
    }
}
