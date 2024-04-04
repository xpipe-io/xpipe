package io.xpipe.app.exchange;

import io.xpipe.beacon.BeaconHandler;
import io.xpipe.beacon.exchange.LaunchExchange;
import io.xpipe.core.store.LaunchableStore;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class LaunchExchangeImpl extends LaunchExchange
        implements MessageExchangeImpl<LaunchExchange.Request, LaunchExchange.Response> {

    @Override
    public Response handleRequest(BeaconHandler handler, Request msg) throws Exception {
        var store = getStoreEntryById(msg.getId(), false);
        if (store.getStore() instanceof LaunchableStore s) {
//            var command = s.prepareLaunchCommand()
//                    .prepareTerminalOpen(TerminalInitScriptConfig.ofName(store.getName()), sc -> null);
//            return Response.builder().command(split(command)).build();
        }

        throw new IllegalArgumentException(store.getName() + " is not launchable");
    }

    private List<String> split(String command) {
        var split = Arrays.stream(command.split(" ", 3)).collect(Collectors.toList());
        var s = split.get(2);
        if ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'"))) {
            split.set(2, s.substring(1, s.length() - 1));
        }
        return split;
    }
}
