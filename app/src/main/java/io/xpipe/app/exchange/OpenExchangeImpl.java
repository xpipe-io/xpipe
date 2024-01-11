package io.xpipe.app.exchange;

import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.launcher.LauncherInput;
import io.xpipe.app.util.PlatformState;
import io.xpipe.beacon.BeaconHandler;
import io.xpipe.beacon.ServerException;
import io.xpipe.beacon.exchange.OpenExchange;

public class OpenExchangeImpl extends OpenExchange
        implements MessageExchangeImpl<OpenExchange.Request, OpenExchange.Response> {

    @Override
    public Response handleRequest(BeaconHandler handler, Request msg) throws ServerException {
        if (msg.getArguments().isEmpty()) {
            if (!OperationMode.switchToSyncIfPossible(OperationMode.GUI)) {
                throw new ServerException(PlatformState.getLastError());
            }
        }

        LauncherInput.handle(msg.getArguments());
        return Response.builder().build();
    }
}
