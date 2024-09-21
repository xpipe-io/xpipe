package io.xpipe.app.beacon.impl;

import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
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

        var entry = DataStoreEntry.createNew(msg.getName(), msg.getData());
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
        return Response.builder().connection(entry.getUuid()).build();
    }
}
