package io.xpipe.app.beacon.api;

import com.sun.net.httpserver.HttpExchange;
import io.xpipe.app.beacon.BeaconClientException;
import io.xpipe.app.beacon.BeaconInterface;

import com.fasterxml.jackson.databind.JsonNode;
import io.xpipe.app.ext.DataStore;
import io.xpipe.app.ext.ValidationException;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.JacksonMapper;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

public class ConnectionAddExchange extends BeaconInterface<ConnectionAddExchange.Request> {

    @Override
    public String getPath() {
        return "/connection/add";
    }

    @Override
    public Object handle(HttpExchange exchange, Request msg) throws Throwable {
        var store = JacksonMapper.getDefault().treeToValue(msg.getData(), DataStore.class);
        if (store == null) {
            throw new BeaconClientException("Unable to parse store data into valid store");
        }

        var foundStore = DataStorage.get().getStoreEntryIfPresent(store, false);
        if (foundStore.isPresent()) {
            return Response.builder().connection(foundStore.get().getUuid()).build();
        }

        var foundName = DataStorage.get().getStoreEntryIfPresent(msg.getName());
        if (foundName.isPresent()) {
            var foundNameStore = foundName.get().getStore();
            // Only allow updates for the same type of store
            if (foundNameStore != null && foundNameStore.getClass().equals(store.getClass())) {
                DataStorage.get().updateEntryStore(foundName.get(), store);
                return Response.builder().connection(foundName.get().getUuid()).build();
            }
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
            } else {
                store.checkComplete();
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

    @Jacksonized
    @Builder
    @Value
    public static class Request {
        @NonNull
        String name;

        @NonNull
        JsonNode data;

        @NonNull
        Boolean validate;

        UUID category;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response {
        @NonNull
        UUID connection;
    }
}
