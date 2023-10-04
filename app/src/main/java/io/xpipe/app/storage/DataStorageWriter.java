package io.xpipe.app.storage;

import com.fasterxml.jackson.databind.JsonNode;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.util.JacksonMapper;

public class DataStorageWriter {

    public static JsonNode storeToNode(DataStore store) {
        var mapper = JacksonMapper.getDefault();
        var tree = mapper.valueToTree(store);
        return tree;
    }
}
