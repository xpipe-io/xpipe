package io.xpipe.ext.jackson.json_table;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import io.xpipe.core.data.node.DataStructureNodeAcceptor;
import io.xpipe.core.data.node.TupleNode;
import io.xpipe.core.impl.SimpleTableWriteConnection;
import io.xpipe.core.impl.StreamWriteConnection;
import io.xpipe.core.source.TableMapping;
import io.xpipe.core.util.JacksonMapper;
import io.xpipe.ext.jackson.JacksonConverter;
import lombok.Getter;

public class JsonTableWriteConnection extends StreamWriteConnection
        implements SimpleTableWriteConnection<JsonTableProvider.Source> {

    @Getter
    private final JsonTableProvider.Source source;

    private JsonGenerator generator;

    public JsonTableWriteConnection(JsonTableProvider.Source source) {
        super(source.getStore(), source.getCharset());
        this.source = source;
    }

    @Override
    public void init() throws Exception {
        super.init();

        JsonFactory f = new JsonFactory();
        generator = f.createGenerator(writer)
                .setPrettyPrinter(new DefaultPrettyPrinter()
                        .withObjectIndenter(new DefaultIndenter()
                                .withLinefeed(source.getNewLine().getNewLineString())));
        generator.setCodec(JacksonMapper.newMapper());

        generator.writeStartArray();
    }

    @Override
    public void close() throws Exception {
        generator.writeEndArray();
        generator.close();
        super.close();
    }

    @Override
    public DataStructureNodeAcceptor<TupleNode> writeLinesAcceptor(TableMapping mapping) {
        return node -> {
            var converted = JacksonConverter.convertToJson(node);
            generator.writeTree(converted);
            return true;
        };
    }
}
