package io.xpipe.app.exchange;

import io.xpipe.app.util.TerminalLauncherManager;
import io.xpipe.beacon.BeaconHandler;
import io.xpipe.beacon.ClientException;
import io.xpipe.beacon.exchange.TerminalLaunchExchange;

public class TerminalLaunchExchangeImpl extends TerminalLaunchExchange
        implements MessageExchangeImpl<TerminalLaunchExchange.Request, TerminalLaunchExchange.Response> {

    @Override
    public Response handleRequest(BeaconHandler handler, Request msg) throws ClientException {
        var r = TerminalLauncherManager.performLaunch(msg.getRequest());
        return Response.builder().targetFile(r).build();
    }
}
