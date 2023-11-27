package io.xpipe.app.exchange;

import io.xpipe.app.core.AppStyle;
import io.xpipe.app.core.AppTheme;
import io.xpipe.app.util.AskpassAlert;
import io.xpipe.beacon.BeaconHandler;
import io.xpipe.beacon.exchange.AskpassExchange;

public class AskpassExchangeImpl extends AskpassExchange
        implements MessageExchangeImpl<AskpassExchange.Request, AskpassExchange.Response> {

    @Override
    public Response handleRequest(BeaconHandler handler, Request msg) {
        AppStyle.init();
        AppTheme.init();
        var r = AskpassAlert.query(msg.getPrompt(), msg.getRequest(), msg.getStoreId(), msg.getSubId());
        return Response.builder().value(r != null ? r.inPlace() : null).build();
    }
}
