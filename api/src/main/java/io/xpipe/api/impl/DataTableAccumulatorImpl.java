package io.xpipe.api.impl;

import io.xpipe.api.DataTable;
import io.xpipe.api.DataTableAccumulator;
import io.xpipe.core.data.node.DataStructureNodeAcceptor;
import io.xpipe.core.data.node.TupleNode;
import io.xpipe.core.source.DataSourceId;

public class DataTableAccumulatorImpl implements DataTableAccumulator {

    @Override
    public DataTable finish(DataSourceId id) {
        return null;
    }

    @Override
    public void add(TupleNode row) {

    }

    @Override
    public DataStructureNodeAcceptor<TupleNode> acceptor() {
        return null;
    }

    @Override
    public int getCurrentRows() {
        return 0;
    }
}
