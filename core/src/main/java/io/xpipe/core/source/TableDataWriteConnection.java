package io.xpipe.core.source;

import io.xpipe.core.data.DataStructureNodeAcceptor;
import io.xpipe.core.data.node.ArrayNode;
import io.xpipe.core.data.node.SimpleTupleNode;

public interface TableDataWriteConnection extends DataSourceConnection {

    DataStructureNodeAcceptor<SimpleTupleNode> writeLinesAcceptor();

    void writeLines(ArrayNode lines) throws Exception;
}
