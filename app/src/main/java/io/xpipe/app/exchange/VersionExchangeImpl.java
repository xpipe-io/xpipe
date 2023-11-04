package io.xpipe.app.exchange;

import io.xpipe.app.core.AppProperties;
import io.xpipe.beacon.BeaconHandler;
import io.xpipe.beacon.exchange.cli.VersionExchange;

public class VersionExchangeImpl extends VersionExchange implements MessageExchangeImpl<VersionExchange.Request, VersionExchange.Response> {

    @Override
    public Response handleRequest(BeaconHandler handler, Request msg) {
        var jvmVersion = System.getProperty("java.vm.vendor") + " " + System.getProperty("java.vm.name") + " (" + System.getProperty(
                "java.vm.version") + ")";
        var version = AppProperties.get().getVersion();
        return Response.builder().version(version).buildVersion(AppProperties.get().getBuild()).jvmVersion(jvmVersion).build();
    }
}
