package io.xpipe.app.exchange;

import io.xpipe.app.ext.DataSourceProvider;
import io.xpipe.app.ext.DataSourceProviders;
import io.xpipe.beacon.BeaconHandler;
import io.xpipe.beacon.ClientException;
import io.xpipe.beacon.exchange.ReadExchange;
import io.xpipe.core.dialog.Dialog;
import io.xpipe.core.dialog.QueryConverter;

import java.util.UUID;

public class ReadExchangeImpl extends ReadExchange
        implements MessageExchangeImpl<ReadExchange.Request, ReadExchange.Response> {

    @Override
    public Response handleRequest(BeaconHandler handler, Request msg) throws Exception {
        var store = resolveStore(msg.getStore(), false);
        DataSourceProvider<?> provider;
        if (msg.getProvider() == null) {
            provider = DataSourceProviders.byPreferredStore(store, null).orElse(null);
        } else {
            provider = getProvider(msg.getProvider());
        }

        var typeQ = Dialog.skipIf(
                Dialog.retryIf(
                        Dialog.query(
                                "Data source type could not be determined.\nSpecify type explicitly",
                                true,
                                true,
                                false,
                                null,
                                QueryConverter.STRING),
                        (String type) -> {
                            return DataSourceProviders.byName(type).isEmpty() ? "Unknown type: " + type : null;
                        }),
                () -> provider != null);

        var config = Dialog.lazy(() -> {
            if (!provider.couldSupportStore(store)) {
                throw new ClientException("Type " + provider.getId() + " does not support store");
            }

            var defaultDesc = provider.createDefaultSource(store);
            return toCompleteConfig(defaultDesc, provider, msg.isConfigureAll());
        });

        var noTarget = msg.getTarget() == null;
        var colName = noTarget ? null : msg.getTarget().getCollectionName();
        var entryName =
                noTarget ? UUID.randomUUID().toString() : msg.getTarget().getEntryName();

        return Response.builder().build();
    }
}
