package io.xpipe.extension.util;

import io.xpipe.core.data.node.DataStructureNodeAcceptor;
import io.xpipe.core.data.node.TupleNode;
import io.xpipe.core.source.DataSource;
import io.xpipe.core.source.DataSourceConnection;
import io.xpipe.core.source.DataSourceType;
import io.xpipe.core.source.TableWriteConnection;

public class AppendingTableWriteConnection extends AppendingWriteConnection implements TableWriteConnection {

    public AppendingTableWriteConnection(DataSource<?> source, DataSourceConnection connection
    ) {
        super(DataSourceType.TABLE, source, connection);
    }

    @Override
    public DataStructureNodeAcceptor<TupleNode> writeLinesAcceptor() {
        return ((TableWriteConnection)connection).writeLinesAcceptor();
    }
}
