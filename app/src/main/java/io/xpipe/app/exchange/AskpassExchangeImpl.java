package io.xpipe.app.exchange;

import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.util.AskpassAlert;
import io.xpipe.beacon.BeaconHandler;
import io.xpipe.beacon.exchange.AskpassExchange;

public class AskpassExchangeImpl extends AskpassExchange implements MessageExchangeImpl<AskpassExchange.Request, AskpassExchange.Response> {

    @Override
    public Response handleRequest(BeaconHandler handler, Request msg) {
        if (OperationMode.get().equals(OperationMode.BACKGROUND)) {
            OperationMode.switchTo(OperationMode.TRAY);
        }

        var r = AskpassAlert.query(msg.getPrompt(), msg.getRequest(), msg.getStoreId(), msg.getSubId());
        return Response.builder().value(r != null ? r.inPlace() : null).build();
    }
}
