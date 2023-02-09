package io.xpipe.core.impl;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import io.xpipe.core.data.node.DataStructureNodeAcceptor;
import io.xpipe.core.data.node.TupleNode;
import io.xpipe.core.data.type.TupleType;
import io.xpipe.core.data.typed.TypedDataStreamWriter;
import io.xpipe.core.source.TableMapping;
import io.xpipe.core.util.JacksonMapper;
import lombok.Getter;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

public class XpbtWriteConnection extends StreamWriteConnection implements SimpleTableWriteConnection<XpbtSource> {

    @Getter
    private final XpbtSource source;

    private TupleType writtenDescriptor;

    public XpbtWriteConnection(XpbtSource source) {
        super(source.getStore(), null);
        this.source = source;
    }

    @Override
    public DataStructureNodeAcceptor<TupleNode> writeLinesAcceptor(TableMapping mapping) {
        return t -> {
            writeDescriptor(t);
            TypedDataStreamWriter.writeStructure(outputStream, t, writtenDescriptor);
            return true;
        };
    }

    private void writeDescriptor(TupleNode tupleNode) throws IOException {
        if (writtenDescriptor != null) {
            return;
        }
        writtenDescriptor = TupleType.tableType(tupleNode.getKeyNames());

        var writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
        JsonFactory f = new JsonFactory();
        try (JsonGenerator g = f.createGenerator(writer)
                .disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET)
                .setPrettyPrinter(new DefaultPrettyPrinter())) {
            JacksonMapper.newMapper()
                    .disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET)
                    .writeValue(g, tupleNode.getKeyNames());
            writer.append("\n");
        }
        writer.flush();
    }
}
