package io.xpipe.core.impl;

import io.xpipe.core.data.node.DataStructureNodeAcceptor;
import io.xpipe.core.data.node.TupleNode;
import io.xpipe.core.data.type.TupleType;
import io.xpipe.core.source.TableReadConnection;

import java.util.OptionalInt;
import java.util.concurrent.atomic.AtomicInteger;

public class LimitTableReadConnection implements TableReadConnection {

    private final TableReadConnection connection;
    private final int maxCount;
    private int count = 0;

    public LimitTableReadConnection(TableReadConnection connection, int maxCount) {
        this.connection = connection;
        this.maxCount = maxCount;
    }

    @Override
    public void init() throws Exception {
        connection.init();
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
        connection.withRows(node -> {
            if (count == maxCount) {
                return false;
            }
            count++;

            var returned = lineAcceptor.accept(node);
            localCounter.getAndIncrement();

            return returned;
        });
        return localCounter.get();
    }

    @Override
    public boolean canRead() throws Exception {
        return connection.canRead();
    }
}
