package io.xpipe.core.impl;

import io.xpipe.core.data.node.ArrayNode;
import io.xpipe.core.data.node.DataStructureNode;
import io.xpipe.core.data.node.DataStructureNodeAcceptor;
import io.xpipe.core.data.node.TupleNode;
import io.xpipe.core.source.TableMapping;
import io.xpipe.core.source.TableWriteConnection;

import java.util.ArrayList;
import java.util.List;

public abstract class BatchTableWriteConnection implements TableWriteConnection {

    public static final int DEFAULT_BATCH_SIZE = 2000;

    protected final int batchSize = DEFAULT_BATCH_SIZE;
    private final List<DataStructureNode> batch = new ArrayList<>();
    private TableMapping mapping;

    @Override
    public final DataStructureNodeAcceptor<TupleNode> writeLinesAcceptor(TableMapping mapping) {
        this.mapping = mapping;
        return node -> {
            if (batch.size() < batchSize) {
                batch.add(node);
                if (batch.size() < batchSize) {
                    return true;
                }
            }

            var array = ArrayNode.of(batch);
            var returned = writeBatchLinesAcceptor(mapping).accept(array);
            batch.clear();
            return returned;
        };
    }

    @Override
    public final void close() throws Exception {
        try {
            if (batch.size() > 0) {
                var array = ArrayNode.of(batch);
                var returned = writeBatchLinesAcceptor(mapping).accept(array);
                batch.clear();
            }
        } finally {
            onClose(mapping);
        }
    }

    protected abstract void onClose(TableMapping mapping) throws Exception;

    protected abstract DataStructureNodeAcceptor<ArrayNode> writeBatchLinesAcceptor(TableMapping mapping);
}
