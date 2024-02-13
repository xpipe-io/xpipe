package io.xpipe.app.exchange;

import io.xpipe.app.util.TerminalLauncherManager;
import io.xpipe.beacon.BeaconHandler;
import io.xpipe.beacon.ClientException;
import io.xpipe.beacon.ServerException;
import io.xpipe.beacon.exchange.TerminalWaitExchange;

public class TerminalWaitExchangeImpl extends TerminalWaitExchange
        implements MessageExchangeImpl<TerminalWaitExchange.Request, TerminalWaitExchange.Response> {

    @Override
    public Response handleRequest(BeaconHandler handler, Request msg) throws ServerException, ClientException {
        TerminalLauncherManager.waitForCompletion(msg.getRequest());
        return Response.builder().build();
    }
}
