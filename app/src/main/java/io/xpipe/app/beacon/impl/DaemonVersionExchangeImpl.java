package io.xpipe.app.beacon.impl;

import io.xpipe.app.core.AppProperties;
import io.xpipe.beacon.api.DaemonVersionExchange;

import com.sun.net.httpserver.HttpExchange;

public class DaemonVersionExchangeImpl extends DaemonVersionExchange {

    @Override
    public boolean requiresCompletedStartup() {
        return false;
    }

    @Override
    public Object handle(HttpExchange exchange, Request msg) {
        var jvmVersion = System.getProperty("java.vm.vendor") + " "
                + System.getProperty("java.vm.name") + " ("
                + System.getProperty("java.vm.version") + ")";
        var version = AppProperties.get().getVersion();
        return Response.builder()
                .version(version)
                .buildVersion(AppProperties.get().getBuild())
                .jvmVersion(jvmVersion)
                .build();
    }
}
