package io.xpipe.ext.jackson.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import io.xpipe.core.data.node.DataStructureNode;
import io.xpipe.core.impl.StreamWriteConnection;
import io.xpipe.core.source.StructureWriteConnection;
import io.xpipe.core.util.JacksonMapper;
import io.xpipe.ext.jackson.JacksonConverter;

public class JsonWriteConnection extends StreamWriteConnection implements StructureWriteConnection {

    private final JsonProvider.Source source;

    public JsonWriteConnection(JsonProvider.Source source) {
        super(source.getStore(), source.getCharset());
        this.source = source;
    }

    @Override
    public void write(DataStructureNode node) throws Exception {
        var convertedNode = JacksonConverter.convertToJson(node);
        JsonFactory f = new JsonFactory();
        try (JsonGenerator g = f.createGenerator(writer)
                .setPrettyPrinter(new DefaultPrettyPrinter()
                        .withObjectIndenter(new DefaultIndenter()
                                .withLinefeed(source.getNewLine().getNewLineString())))) {
            JacksonMapper.newMapper().writeTree(g, convertedNode);
        }
    }
}
