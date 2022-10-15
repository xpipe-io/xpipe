package io.xpipe.core.impl;

import io.xpipe.core.data.node.DataStructureNodeAcceptor;
import io.xpipe.core.data.node.TupleNode;
import io.xpipe.core.source.DataSource;
import io.xpipe.core.source.DataSourceConnection;
import io.xpipe.core.source.DataSourceType;
import io.xpipe.core.source.TableWriteConnection;

public class PreservingTableWriteConnection extends PreservingWriteConnection implements TableWriteConnection {

    public PreservingTableWriteConnection(DataSource<?> source, DataSourceConnection connection, boolean append) {
        super(DataSourceType.TABLE, source, append, connection);
    }

    @Override
    public DataStructureNodeAcceptor<TupleNode> writeLinesAcceptor() {
        return ((TableWriteConnection) connection).writeLinesAcceptor();
    }
}
