package io.xpipe.core.impl;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import io.xpipe.core.data.node.DataStructureNodeAcceptor;
import io.xpipe.core.data.node.TupleNode;
import io.xpipe.core.data.type.TupleType;
import io.xpipe.core.data.typed.TypedDataStreamParser;
import io.xpipe.core.data.typed.TypedDataStructureNodeReader;
import io.xpipe.core.source.StreamReadConnection;
import io.xpipe.core.source.TableReadConnection;
import io.xpipe.core.store.StreamDataStore;
import io.xpipe.core.util.JacksonMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class XpbtReadConnection extends StreamReadConnection implements TableReadConnection {

    private final StreamDataStore store;
    private TupleType dataType;
    private TypedDataStreamParser parser;
    private boolean empty;
    protected XpbtReadConnection(XpbtSource source) {
        super(source.getStore(), null);
        this.store = source.getStore();
    }

    @Override
    public void init() throws Exception {
        super.init();

        this.inputStream.mark(8192);
        var header = new BufferedReader(new InputStreamReader(inputStream)).readLine();
        this.inputStream.reset();
        if (header == null || header.trim().length() == 0) {
            this.dataType = TupleType.empty();
            empty = true;
            return;
        }

        var headerLength = header.getBytes(StandardCharsets.UTF_8).length;
        this.inputStream.skip(headerLength + 1);
        List<String> names = JacksonMapper.newMapper()
                .disable(JsonParser.Feature.AUTO_CLOSE_SOURCE)
                .readerFor(new TypeReference<List<String>>() {})
                .readValue(header);
        TupleType dataType = TupleType.tableType(names);
        this.dataType = dataType;
        this.parser = new TypedDataStreamParser(dataType);
    }
    
    @Override
    public TupleType getDataType() {
        return dataType;
    }

    @Override
    public void withRows(DataStructureNodeAcceptor<TupleNode> lineAcceptor) throws Exception {
        if (empty) {
            return;
        }

        var reader = TypedDataStructureNodeReader.of(dataType);
        AtomicBoolean quit = new AtomicBoolean(false);
        AtomicReference<Exception> exception = new AtomicReference<>();
        while (!quit.get()) {
            var node = parser.parseStructure(inputStream, reader);
            if (node == null) {
                quit.set(true);
                break;
            }

            try {
                if (!lineAcceptor.accept(node.asTuple())) {
                    quit.set(true);
                }
            } catch (Exception ex) {
                quit.set(true);
                exception.set(ex);
            }
        }

        if (exception.get() != null) {
            throw exception.get();
        }
    }

    @Override
    public boolean canRead() throws Exception {
        return store.canOpen();
    }
}
