package io.xpipe.core.impl;

import io.xpipe.core.charsetter.StreamCharset;
import io.xpipe.core.data.node.DataStructureNodeAcceptor;
import io.xpipe.core.data.node.TupleNode;
import io.xpipe.core.data.type.TupleType;
import io.xpipe.core.source.TableReadConnection;
import io.xpipe.core.store.StreamDataStore;

import java.util.concurrent.atomic.AtomicReference;

public abstract class PeekTableStreamReadConnection extends StreamReadConnection implements TableReadConnection {

    private TupleNode first;
    private TupleType type;

    public PeekTableStreamReadConnection(StreamDataStore store, StreamCharset charset) {
        super(store, charset);
    }

    @Override
    public void init() throws Exception {
        super.init();
        AtomicReference<TupleNode> read = new AtomicReference<>();
        withRowsInternal(node -> {
            read.set(node);
            return false;
        });
        if (read.get() == null) {
            return;
        }

        first = read.get().asTuple();
        type = convertType(first);
    }

    protected TupleType convertType(TupleNode n) {
        return n.determineDataType().asTuple();
    }

    @Override
    public void close() throws Exception {
        if (inputStream == null) {
            throw new IllegalStateException("Not initialized");
        }

        inputStream.close();
    }

    @Override
    public TupleType getDataType() {
        return type;
    }

    @Override
    public final void withRows(DataStructureNodeAcceptor<TupleNode> lineAcceptor) throws Exception {
        if (first != null) {
            lineAcceptor.accept(first);
            first = null;
        }

        withRowsInternal(lineAcceptor);
    }

    protected abstract void withRowsInternal(DataStructureNodeAcceptor<TupleNode> lineAcceptor) throws Exception;
}
