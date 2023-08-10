package io.xpipe.app.exchange;

import io.xpipe.app.ext.DataSourceProvider;
import io.xpipe.app.ext.DataSourceProviders;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.beacon.BeaconHandler;
import io.xpipe.beacon.ClientException;
import io.xpipe.beacon.RequestMessage;
import io.xpipe.beacon.ResponseMessage;
import io.xpipe.beacon.exchange.MessageExchange;
import io.xpipe.core.dialog.Dialog;
import io.xpipe.core.impl.NamedStore;
import io.xpipe.core.source.DataSource;
import io.xpipe.core.source.DataStoreId;
import io.xpipe.core.store.DataStore;
import lombok.NonNull;

import java.util.Optional;

public interface MessageExchangeImpl<RQ extends RequestMessage, RS extends ResponseMessage> extends MessageExchange {

    @SuppressWarnings("unchecked")
    default <T extends DataSource<?>> Dialog toCompleteConfig(
            @NonNull DataSource<?> source, @NonNull DataSourceProvider<T> p, boolean all) throws ClientException {
        var dialog = p.configDialog((T) source, all);
        if (dialog == null) {
            throw new ClientException(
                    String.format("Data source of type %s does not support editing from the CLI", p.getId()));
        }
        return dialog;
    }

    default DataSourceProvider<?> getProvider(@NonNull String id) throws ClientException {
        Optional<DataSourceProvider<?>> prov = DataSourceProviders.byName(id);
        if (prov.isEmpty()) {
            throw new ClientException("No matching data source type found for type " + id);
        }
        return prov.get();
    }

    default DataStore resolveStore(@NonNull DataStore in, boolean acceptDisabled) throws ClientException {
        try {
            if (in instanceof NamedStore n) {
                var found = DataStorage.get().getStoreEntry(n.getName(), acceptDisabled);
                return found.getStore();
            }
        } catch (IllegalArgumentException ex) {
            throw new ClientException(ex.getMessage());
        }

        return in;
    }

    default DataStoreEntry getStoreEntryByName(@NonNull String name, boolean acceptDisabled) throws ClientException {
        var store = DataStorage.get().getStoreEntryIfPresent(name);
        if (store.isEmpty()) {
            throw new ClientException("No store with name " + name + " was found");
        }
        if (store.get().isDisabled() && !acceptDisabled) {
            throw new ClientException(
                    String.format("Store %s is disabled", store.get().getName()));
        }
        return store.get();
    }

    default DataStoreEntry getStoreEntryById(@NonNull DataStoreId id, boolean acceptUnusable) throws ClientException {
        var store = DataStorage.get().getStoreEntryIfPresent(id);
        if (store.isEmpty()) {
            throw new ClientException("No store with id " + id + " was found");
        }
        if (store.get().isDisabled() && !acceptUnusable) {
            throw new ClientException(
                    String.format("Store %s is disabled", store.get().getName()));
        }
        if (!store.get().getState().isUsable() && !acceptUnusable) {
            throw new ClientException(
                    String.format("Store %s is not completely configured", store.get().getName()));
        }
        return store.get();
    }

    String getId();

    RS handleRequest(BeaconHandler handler, RQ msg) throws Exception;
}
