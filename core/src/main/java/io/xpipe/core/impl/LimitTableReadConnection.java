package io.xpipe.core.impl;

import io.xpipe.core.data.node.DataStructureNodeAcceptor;
import io.xpipe.core.data.node.TupleNode;
import io.xpipe.core.data.type.TupleType;
import io.xpipe.core.source.TableReadConnection;

import java.util.OptionalInt;

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
    public void withRows(DataStructureNodeAcceptor<TupleNode> lineAcceptor) throws Exception {
        connection.withRows(node -> {
            if (count == maxCount) {
                return false;
            }
            count++;

            return lineAcceptor.accept(node);
        });
    }

    @Override
    public boolean canRead() throws Exception {
        return connection.canRead();
    }
}
