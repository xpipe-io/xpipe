package io.xpipe.app.beacon.impl;

import io.xpipe.app.core.AppOpenArguments;
import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.util.PlatformInit;
import io.xpipe.beacon.BeaconServerException;
import io.xpipe.beacon.api.DaemonOpenExchange;
import io.xpipe.core.process.OsType;

import com.sun.net.httpserver.HttpExchange;

public class DaemonOpenExchangeImpl extends DaemonOpenExchange {

    private int openCounter = 0;

    @Override
    public Object handle(HttpExchange exchange, Request msg) throws BeaconServerException {
        if (msg.getArguments().isEmpty()) {
            try {
                // At this point we are already loading this on another thread
                // so this call will only perform the waiting
                PlatformInit.init(true);
            } catch (Throwable t) {
                throw new BeaconServerException(t);
            }

            // The open command is used as a default opener on Linux
            // We don't want to overwrite the default startup mode
            if (OsType.getLocal() == OsType.LINUX && openCounter++ == 0) {
                return Response.builder().build();
            }

            OperationMode.switchToAsync(OperationMode.GUI);
        } else {
            AppOpenArguments.handle(msg.getArguments());
        }
        return Response.builder().build();
    }

    @Override
    public boolean requiresEnabledApi() {
        return false;
    }

    @Override
    public boolean requiresCompletedStartup() {
        return false;
    }
}
