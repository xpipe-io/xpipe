package io.xpipe.core.source;

import io.xpipe.core.data.node.DataStructureNodeAcceptor;
import io.xpipe.core.data.node.TupleNode;
import io.xpipe.core.data.type.TupleType;

import java.util.Optional;

/**
 * A connection for sequentially writing data to a table data source.
 */
public interface TableWriteConnection extends DataSourceConnection {

    public static TableWriteConnection empty() {
        return new TableWriteConnection() {
            @Override
            public Optional<TableMapping> createMapping(TupleType inputType) throws Exception {
                return Optional.of(TableMapping.empty(inputType));
            }

            @Override
            public DataStructureNodeAcceptor<TupleNode> writeLinesAcceptor(TableMapping mapping) {
                return node -> {
                    return true;
                };
            }
        };
    }

    Optional<TableMapping> createMapping(TupleType inputType) throws Exception;

    DataStructureNodeAcceptor<TupleNode> writeLinesAcceptor(TableMapping mapping);
}
