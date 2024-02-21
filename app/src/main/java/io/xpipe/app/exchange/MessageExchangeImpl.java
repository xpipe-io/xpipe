package io.xpipe.app.exchange;

import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.beacon.BeaconHandler;
import io.xpipe.beacon.ClientException;
import io.xpipe.beacon.RequestMessage;
import io.xpipe.beacon.ResponseMessage;
import io.xpipe.beacon.exchange.MessageExchange;
import io.xpipe.core.store.DataStoreId;
import lombok.NonNull;

public interface MessageExchangeImpl<RQ extends RequestMessage, RS extends ResponseMessage> extends MessageExchange {

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
        if (!store.get().getValidity().isUsable() && !acceptUnusable) {
            throw new ClientException(String.format(
                    "Store %s is not completely configured", store.get().getName()));
        }
        return store.get();
    }

    String getId();

    RS handleRequest(BeaconHandler handler, RQ msg) throws Exception;
}
