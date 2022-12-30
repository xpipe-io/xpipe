package io.xpipe.core.impl;

import io.xpipe.core.data.node.DataStructureNodeAcceptor;
import io.xpipe.core.data.node.TupleNode;
import io.xpipe.core.data.type.TupleType;
import io.xpipe.core.source.*;

import java.util.Optional;

public class PreservingTableWriteConnection extends PreservingWriteConnection implements TableWriteConnection {

    public PreservingTableWriteConnection(DataSource<?> source, DataSourceConnection connection, boolean append) {
        super(DataSourceType.TABLE, source, append, connection);
    }

    @Override
    public Optional<TableMapping> createMapping(TupleType inputType) throws Exception {
        return ((TableWriteConnection) connection).createMapping(inputType);
    }

    @Override
    public DataStructureNodeAcceptor<TupleNode> writeLinesAcceptor(TableMapping mapping) {
        return ((TableWriteConnection) connection).writeLinesAcceptor(mapping);
    }
}
