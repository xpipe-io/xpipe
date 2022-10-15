package io.xpipe.core.impl;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import io.xpipe.core.data.node.DataStructureNodeAcceptor;
import io.xpipe.core.data.node.TupleNode;
import io.xpipe.core.data.type.TupleType;
import io.xpipe.core.data.typed.TypedDataStreamWriter;
import io.xpipe.core.source.TableWriteConnection;
import io.xpipe.core.store.StreamDataStore;
import io.xpipe.core.util.JacksonMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

public class XpbtWriteConnection implements TableWriteConnection {

    private final StreamDataStore store;
    private OutputStream outputStream;
    private TupleType writtenDescriptor;

    public XpbtWriteConnection(StreamDataStore store) {
        this.store = store;
    }

    @Override
    public void init() throws Exception {
        outputStream = store.openOutput();
    }

    @Override
    public void close() throws Exception {
        if (outputStream != null) {
            outputStream.close();
        }
    }

    @Override
    public DataStructureNodeAcceptor<TupleNode> writeLinesAcceptor() {
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
