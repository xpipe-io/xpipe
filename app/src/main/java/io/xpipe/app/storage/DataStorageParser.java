package io.xpipe.app.storage;

import com.fasterxml.jackson.databind.JsonNode;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.util.JacksonMapper;

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
