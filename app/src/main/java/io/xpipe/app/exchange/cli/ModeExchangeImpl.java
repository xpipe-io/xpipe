package io.xpipe.app.exchange.cli;

import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.exchange.MessageExchangeImpl;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.beacon.BeaconHandler;
import io.xpipe.beacon.ClientException;
import io.xpipe.beacon.exchange.cli.ModeExchange;

public class ModeExchangeImpl extends ModeExchange
        implements MessageExchangeImpl<ModeExchange.Request, ModeExchange.Response> {

    @Override
    public Response handleRequest(BeaconHandler handler, Request msg) throws Exception {
        // Wait for startup
        while (OperationMode.get() == null) {
            ThreadHelper.sleep(100);
        }

        var mode = OperationMode.map(msg.getMode());
        if (!mode.isSupported()) {
            throw new ClientException("Unsupported mode: " + msg.getMode().getDisplayName() + ". Supported: "
                    + String.join(
                            ", ",
                            OperationMode.getAll().stream()
                                    .filter(OperationMode::isSupported)
                                    .map(OperationMode::getId)
                                    .toList()));
        }

        OperationMode.switchToSyncIfPossible(mode);
        return ModeExchange.Response.builder()
                .usedMode(OperationMode.map(OperationMode.get()))
                .build();
    }
}
