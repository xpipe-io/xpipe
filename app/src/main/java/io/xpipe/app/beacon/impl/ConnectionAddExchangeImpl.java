package io.xpipe.app.beacon.impl;

import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.beacon.BeaconClientException;
import io.xpipe.beacon.api.ConnectionAddExchange;
import io.xpipe.core.util.ValidationException;

import com.sun.net.httpserver.HttpExchange;

public class ConnectionAddExchangeImpl extends ConnectionAddExchange {

    @Override
    public Object handle(HttpExchange exchange, Request msg) throws Throwable {
        var found = DataStorage.get().getStoreEntryIfPresent(msg.getData(), false);
        if (found.isPresent()) {
            return Response.builder().connection(found.get().getUuid()).build();
        }

        if (msg.getCategory() != null
                && DataStorage.get()
                        .getStoreCategoryIfPresent(msg.getCategory())
                        .isEmpty()) {
            throw new BeaconClientException("Category with id " + msg.getCategory() + " does not exist");
        }

        var entry = DataStoreEntry.createNew(msg.getName(), msg.getData());
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
                ErrorEvent.expected(ex);
            } else if (ex instanceof StackOverflowError) {
                // Cycles in connection graphs can fail hard but are expected
                ErrorEvent.expected(ex);
            }
            throw ex;
        } finally {
            DataStorage.get().removeStoreEntryInProgress(entry);
        }
        DataStorage.get().addStoreEntryIfNotPresent(entry);

        // Explicitly assign category
        if (msg.getCategory() != null) {
            DataStorage.get()
                    .updateCategory(
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
