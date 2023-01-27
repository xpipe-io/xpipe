package io.xpipe.ext.jackson.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.xpipe.core.data.node.DataStructureNode;
import io.xpipe.core.impl.StreamReadConnection;
import io.xpipe.core.source.StructureReadConnection;
import io.xpipe.core.util.JacksonMapper;
import io.xpipe.ext.jackson.JacksonConverter;

public class JsonReadConnection extends StreamReadConnection implements StructureReadConnection {

    public JsonReadConnection(JsonProvider.Source source) {
        super(source.getStore(), source.getCharset());
    }

    @Override
    public DataStructureNode read() throws Exception {
        ObjectMapper o = JacksonMapper.newMapper();
        var node = o.readTree(reader);
        return JacksonConverter.convertFromJson(node);
    }
}
