package io.xpipe.app.exchange.cli;

import io.xpipe.app.exchange.MessageExchangeImpl;
import io.xpipe.app.ext.DataSourceProviders;
import io.xpipe.beacon.BeaconHandler;
import io.xpipe.beacon.exchange.cli.SourceProviderListExchange;
import io.xpipe.beacon.exchange.data.ProviderEntry;
import io.xpipe.core.source.DataSourceType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class SourceProviderListExchangeImpl extends SourceProviderListExchange
        implements MessageExchangeImpl<SourceProviderListExchange.Request, SourceProviderListExchange.Response> {

    @Override
    public Response handleRequest(BeaconHandler handler, Request msg) throws Exception {
        var all = DataSourceProviders.getAll();
        var map = new LinkedHashMap<DataSourceType, List<ProviderEntry>>();
        for (DataSourceType t : DataSourceType.values()) {
            map.put(t, new ArrayList<>());
        }

        for (var p : all) {
            map.get(p.getPrimaryType())
                    .add(ProviderEntry.builder()
                            .id(p.getPossibleNames().get(0))
                            .description(p.getDisplayDescription())
                            .build());
        }

        return Response.builder().entries(map).build();
    }
}
