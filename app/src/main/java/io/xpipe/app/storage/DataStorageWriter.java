package io.xpipe.app.storage;

import io.xpipe.core.store.DataStore;
import io.xpipe.core.util.JacksonMapper;

import com.fasterxml.jackson.databind.JsonNode;

public class DataStorageWriter {

    public static JsonNode storeToNode(DataStore store) {
        var mapper = JacksonMapper.getDefault();
        return mapper.valueToTree(store);
    }
}
