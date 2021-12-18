package io.xpipe.core.source;

import io.xpipe.core.data.node.DataStructureNodeAcceptor;
import io.xpipe.core.data.node.ArrayNode;
import io.xpipe.core.data.node.TupleNode;

public interface TableDataWriteConnection extends DataSourceConnection {

    DataStructureNodeAcceptor<TupleNode> writeLinesAcceptor();

    void writeLines(ArrayNode lines) throws Exception;
}
