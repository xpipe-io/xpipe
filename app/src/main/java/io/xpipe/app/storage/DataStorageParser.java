package io.xpipe.app.storage;

import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.util.JacksonMapper;

import com.fasterxml.jackson.databind.JsonNode;

public class DataStorageParser {

    public static DataStore storeFromNode(JsonNode node) {
        var mapper = JacksonMapper.getDefault();
        try {
            return mapper.treeToValue(node, DataStore.class);
        } catch (Throwable e) {
            ErrorEvent.fromThrowable(e).handle();
            return null;
        }
    }
}
