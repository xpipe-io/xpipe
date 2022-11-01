package io.xpipe.core.impl;

import io.xpipe.core.data.node.ArrayNode;
import io.xpipe.core.data.node.DataStructureNodeAcceptor;
import io.xpipe.core.data.node.TupleNode;
import io.xpipe.core.data.type.TupleType;
import io.xpipe.core.source.TableReadConnection;

import java.util.OptionalInt;
import java.util.concurrent.atomic.AtomicInteger;

public class BufferedTableReadConnection implements TableReadConnection {

    private final TableReadConnection connection;
    private final int maxCount;
    private int count = 0;
    private ArrayNode read;

    public BufferedTableReadConnection(TableReadConnection connection, int maxCount) throws Exception {
        this.connection = connection;
        this.maxCount = maxCount;
        read = connection.readRows(maxCount);
    }

    private TupleNode get() throws Exception {
        if (count == read.size()) {
            read = connection.readRows(maxCount);
        }

        if (read.size() == 0) {
            return null;
        }

        return read.at(count++).asTuple();
    }

    @Override
    public void close() throws Exception {
        connection.close();
    }

    @Override
    public TupleType getDataType() {
        return connection.getDataType();
    }

    @Override
    public OptionalInt getRowCount() throws Exception {
        return connection.getRowCount();
    }

    @Override
    public int withRows(DataStructureNodeAcceptor<TupleNode> lineAcceptor) throws Exception {
        AtomicInteger localCounter = new AtomicInteger();
        TupleNode node;
        while (((node = get()) != null)) {
            var returned = lineAcceptor.accept(node);
            if (!returned) {
                break;
            }

            localCounter.getAndIncrement();
        }
        return localCounter.get();
    }

    @Override
    public boolean canRead() throws Exception {
        return connection.canRead();
    }
}
