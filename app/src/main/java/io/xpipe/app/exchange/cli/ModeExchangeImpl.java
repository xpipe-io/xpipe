package io.xpipe.app.exchange.cli;

import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.exchange.MessageExchangeImpl;
import io.xpipe.beacon.BeaconHandler;
import io.xpipe.beacon.ClientException;
import io.xpipe.beacon.exchange.cli.ModeExchange;

public class ModeExchangeImpl extends ModeExchange implements MessageExchangeImpl<ModeExchange.Request, ModeExchange.Response> {

    @Override
    public Response handleRequest(BeaconHandler handler, Request msg) throws Exception {
        if (OperationMode.get() == null) {
            throw new ClientException("Mode switch already in progress");
        }

        var mode = OperationMode.map(msg.getMode());
        if (!mode.isSupported()) {
            throw new ClientException("Unsupported mode: " + msg.getMode().getDisplayName() + ". Supported: " +
                    String.join(", ", OperationMode.getAll().stream().filter(OperationMode::isSupported).map(OperationMode::getId).toList()));
        }

        OperationMode.switchTo(mode);
        return ModeExchange.Response.builder().usedMode(OperationMode.map(OperationMode.get())).build();
    }
}
