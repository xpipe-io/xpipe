package io.xpipe.core.source;

import io.xpipe.core.data.node.DataStructureNode;
import io.xpipe.core.data.node.DataStructureNodeAcceptor;
import io.xpipe.core.data.node.ArrayNode;
import io.xpipe.core.data.node.TupleNode;

/**
 * A connection for sequentially writing data to a table data source.
 */
public interface TableWriteConnection extends DataSourceConnection {

    DataStructureNodeAcceptor<TupleNode> writeLinesAcceptor();

    default void writeLines(ArrayNode lines) throws Exception{
        var consumer = writeLinesAcceptor();
        for (DataStructureNode dataStructureNode : lines.getNodes()) {
            consumer.accept(dataStructureNode.asTuple());
        }
    }
}
