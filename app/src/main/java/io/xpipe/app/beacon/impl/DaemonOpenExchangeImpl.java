package io.xpipe.app.beacon.impl;

import io.xpipe.app.core.AppOpenArguments;
import io.xpipe.app.core.mode.AppOperationMode;
import io.xpipe.app.platform.PlatformInit;
import io.xpipe.beacon.BeaconServerException;
import io.xpipe.beacon.api.DaemonOpenExchange;
import io.xpipe.core.OsType;

import com.sun.net.httpserver.HttpExchange;

public class DaemonOpenExchangeImpl extends DaemonOpenExchange {

    private int openCounter = 0;

    @Override
    public boolean requiresCompletedStartup() {
        return false;
    }

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
            if (OsType.ofLocal() == OsType.LINUX && openCounter++ == 0) {
                return Response.builder().build();
            }

            AppOperationMode.switchToAsync(AppOperationMode.GUI);
        } else {
            AppOpenArguments.handle(msg.getArguments());
        }
        return Response.builder().build();
    }

    @Override
    public boolean requiresEnabledApi() {
        return false;
    }
}
