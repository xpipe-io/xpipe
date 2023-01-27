package io.xpipe.app.exchange;

import io.xpipe.app.storage.DataStorage;
import io.xpipe.beacon.BeaconHandler;
import io.xpipe.beacon.ClientException;
import io.xpipe.beacon.exchange.cli.ConvertExchange;
import io.xpipe.core.dialog.Dialog;
import io.xpipe.core.source.DataSource;
import io.xpipe.extension.DataSourceProvider;
import io.xpipe.extension.DataSourceProviders;

public class ConvertExchangeImpl extends ConvertExchange
        implements MessageExchangeImpl<ConvertExchange.Request, ConvertExchange.Response> {

    @Override
    public Response handleRequest(BeaconHandler handler, Request msg) throws Exception {
        var ds = getSourceEntry(msg.getRef(), null, false);

        DataSourceProvider<?> newProvider;
        DataSource<?> newSource;
        if (msg.getNewProvider() != null && msg.getNewCategory() != null) {
            var provider = getProvider(msg.getNewProvider());
            if (!provider.getPrimaryType().equals(msg.getNewCategory())) {
                throw new ClientException("Incompatible types: " + provider.getId() + " and "
                        + msg.getNewCategory().name().toLowerCase());
            }

            newProvider = provider;
            newSource = newProvider.createDefaultSource(ds.getStore().asNeeded());
        } else if (msg.getNewProvider() != null) {
            if (msg.getNewProvider().equals(ds.getProvider().getId())) {
                return ConvertExchange.Response.builder().build();
            }

            newProvider = getProvider(msg.getNewProvider());
            newSource = newProvider.createDefaultSource(ds.getStore().asNeeded());
        } else if (msg.getNewCategory() != null) {
            if (msg.getNewCategory().equals(ds.getProvider().getPrimaryType())) {
                return ConvertExchange.Response.builder().build();
            }

            var provider = ds.getProvider();
            DataSource<?> s = ds.getSource();
            if (!ds.getProvider().supportsConversion(s.asNeeded(), msg.getNewCategory())) {
                throw new ClientException(
                        "Data source of type " + provider.getId() + " can not be converted to category "
                                + msg.getNewCategory().name().toLowerCase());
            }

            newSource = provider.convert(s.asNeeded(), msg.getNewCategory());
            newProvider = DataSourceProviders.byDataSourceClass(newSource.getClass());
        } else {
            throw new ClientException("No data format or data type specified");
        }

        var dialog = toCompleteConfig(newSource, newProvider, true);
        var id = DataStorage.get().getId(ds);
        var sent = Dialog.chain(
                        dialog,
                        Dialog.header("Successfully converted " + (id != null ? id : "source") + " to "
                                + newProvider.getDisplayName()))
                .evaluateTo(dialog);
        var ref = DialogExchangeImpl.add(sent, (DataSource<?> o) -> {
            ds.setSource(o);
            DataStorage.get().save();
        });

        return ConvertExchange.Response.builder().config(ref).build();
    }
}
