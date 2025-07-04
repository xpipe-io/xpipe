package io.xpipe.app.beacon.impl;

import io.xpipe.app.ext.DataStore;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.beacon.BeaconClientException;
import io.xpipe.beacon.api.ConnectionAddExchange;
import io.xpipe.app.ext.ValidationException;

import com.sun.net.httpserver.HttpExchange;
import io.xpipe.core.JacksonMapper;

public class ConnectionAddExchangeImpl extends ConnectionAddExchange {

    @Override
    public Object handle(HttpExchange exchange, Request msg) throws Throwable {
        var store = JacksonMapper.getDefault().treeToValue(msg.getData(), DataStore.class);
        if (store == null) {
            throw new BeaconClientException("Unable to parse store data into valid store");
        }

        var found = DataStorage.get().getStoreEntryIfPresent(store, false);
        if (found.isEmpty()) {
            found = DataStorage.get().getStoreEntryIfPresent(msg.getName());
        }

        if (found.isPresent()) {
            found.get().setStoreInternal(store, true);
            return Response.builder().connection(found.get().getUuid()).build();
        }

        if (msg.getCategory() != null
                && DataStorage.get()
                        .getStoreCategoryIfPresent(msg.getCategory())
                        .isEmpty()) {
            throw new BeaconClientException("Category with id " + msg.getCategory() + " does not exist");
        }

        var entry = DataStoreEntry.createNew(msg.getName(), store);
        if (msg.getCategory() != null) {
            entry.setCategoryUuid(msg.getCategory());
        }
        try {
            DataStorage.get().addStoreEntryInProgress(entry);
            if (msg.getValidate()) {
                entry.validateOrThrow();
            }
        } catch (Throwable ex) {
            if (ex instanceof ValidationException) {
                ErrorEventFactory.expected(ex);
            } else if (ex instanceof StackOverflowError) {
                // Cycles in connection graphs can fail hard but are expected
                ErrorEventFactory.expected(ex);
            }
            throw ex;
        } finally {
            DataStorage.get().removeStoreEntryInProgress(entry);
        }
        DataStorage.get().addStoreEntryIfNotPresent(entry);

        // Explicitly assign category
        if (msg.getCategory() != null) {
            DataStorage.get()
                    .moveEntryToCategory(
                            entry,
                            DataStorage.get()
                                    .getStoreCategoryIfPresent(msg.getCategory())
                                    .orElseThrow());
        }

        return Response.builder().connection(entry.getUuid()).build();
    }

    @Override
    public Object getSynchronizationObject() {
        return DataStorage.get();
    }
}
