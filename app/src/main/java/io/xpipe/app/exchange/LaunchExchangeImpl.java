package io.xpipe.app.exchange;

import io.xpipe.beacon.BeaconHandler;
import io.xpipe.beacon.exchange.LaunchExchange;
import io.xpipe.core.store.LaunchableStore;
import org.apache.commons.exec.CommandLine;

import java.util.List;

public class LaunchExchangeImpl extends LaunchExchange
        implements MessageExchangeImpl<LaunchExchange.Request, LaunchExchange.Response> {

    @Override
    public Response handleRequest(BeaconHandler handler, Request msg) throws Exception {
        var store = getStoreEntryByName(msg.getName(), false);
        if (store.getStore() instanceof LaunchableStore s) {
            var command = s.prepareLaunchCommand();
            var split = CommandLine.parse(command);
            return Response.builder().command(List.of(split.toStrings())).build();
        }

        throw new IllegalArgumentException();
    }
}
