package io.xpipe.app.exchange;

import io.xpipe.app.ext.DataSourceProvider;
import io.xpipe.app.ext.DataSourceProviders;
import io.xpipe.beacon.BeaconHandler;
import io.xpipe.beacon.ClientException;
import io.xpipe.beacon.exchange.cli.WritePreparationExchange;
import io.xpipe.core.source.DataSource;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WritePreparationExchangeImpl extends WritePreparationExchange
        implements MessageExchangeImpl<WritePreparationExchange.Request, WritePreparationExchange.Response> {

    public static final Map<UUID, DataSource<?>> CONFIGS = new HashMap<>();

    @Override
    public WritePreparationExchange.Response handleRequest(BeaconHandler handler, Request msg) throws Exception {
        var store = resolveStore(msg.getOutputStore(), false);
        var provider = DataSourceProviders.byPreferredStore(store, null);

        DataSourceProvider<?> outputProvider;
        DataSource<?> source;

        if (msg.getOutputSource() == null) {
            if (msg.getType() == null && provider.isEmpty()) {
                var entry = getSourceEntry(msg.getSource(), null, false);
                var sourceType = entry.getProvider();
                if (sourceType.couldSupportStore(msg.getOutputStore())) {
                    outputProvider = sourceType;
                } else {
                    throw new ClientException("Missing output type");
                }
            } else if (msg.getType() != null) {
                outputProvider = DataSourceProviders.byName(msg.getType())
                        .orElseThrow(() -> new ClientException("Unknown output format type: " + msg.getType()));
            } else if (provider.isPresent()) {
                outputProvider = provider.get();
            } else {
                throw new IllegalStateException();
            }

            if (!outputProvider.couldSupportStore(store)) {
                throw new ClientException("Unsupported store type");
            }
            source = outputProvider.createDefaultSource(store);
        } else {
            source = getSourceEntry(msg.getOutputSource(), null, false).getSource();
            outputProvider = DataSourceProviders.byDataSourceClass(source.getClass());
        }

        var id = UUID.randomUUID();
        var config = toCompleteConfig(source, outputProvider, false);
        var configRef = DialogExchangeImpl.add(config, id, (DataSource<?> s) -> {
            CONFIGS.put(id, s);
        });

        return Response.builder().config(configRef).build();
    }
}
