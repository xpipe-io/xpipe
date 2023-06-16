package io.xpipe.app.exchange.cli;

import io.xpipe.app.exchange.MessageExchangeImpl;
import io.xpipe.app.ext.DataStoreProvider;
import io.xpipe.app.ext.DataStoreProviders;
import io.xpipe.beacon.BeaconHandler;
import io.xpipe.beacon.exchange.cli.StoreProviderListExchange;
import io.xpipe.beacon.exchange.data.ProviderEntry;

import java.util.Arrays;
import java.util.stream.Collectors;

public class StoreProviderListExchangeImpl extends StoreProviderListExchange
        implements MessageExchangeImpl<StoreProviderListExchange.Request, StoreProviderListExchange.Response> {

    @Override
    public Response handleRequest(BeaconHandler handler, Request msg) {
        var categories = DataStoreProvider.DataCategory.values();
        var all = DataStoreProviders.getAll();
        var map = Arrays.stream(categories)
                .collect(Collectors.toMap(category -> getName(category), category -> all.stream()
                        .filter(dataStoreProvider ->
                                dataStoreProvider.getCategory().equals(category))
                        .map(p -> ProviderEntry.builder()
                                .id(p.getId())
                                .description(p.getDisplayDescription())
                                .hidden(!p.shouldShow())
                                .build())
                        .toList()));

        return Response.builder().entries(map).build();
    }

    private String getName(DataStoreProvider.DataCategory category) {
        return category.name().substring(0, 1).toUpperCase()
                + category.name().substring(1).toLowerCase();
    }
}
