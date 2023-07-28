package io.xpipe.api;

import io.xpipe.api.impl.DataTableAccumulatorImpl;
import io.xpipe.core.data.node.DataStructureNode;
import io.xpipe.core.data.node.DataStructureNodeAcceptor;
import io.xpipe.core.data.type.TupleType;
import io.xpipe.core.source.DataStoreId;

/**
 * An accumulator for table data.
 * <p>
 * This class can be used to construct new table data sources by
 * accumulating the rows using {@link #add(DataStructureNode)} or {@link #acceptor()} and then calling
 * {@link #finish(DataStoreId)} to complete the construction process and create a new data source.
 */
public interface DataTableAccumulator {

    static DataTableAccumulator create(TupleType type) {
        return new DataTableAccumulatorImpl(type);
    }

    /**
     * Wrapper for {@link #finish(DataStoreId)}.
     */
    default DataTable finish(String id) {
        return finish(DataStoreId.fromString(id));
    }

    /**
     * Finishes the construction process and returns the data source reference.
     *
     * @param id the data source id to assign
     */
    DataTable finish(DataStoreId id);

    /**
     * Adds a row to the table.
     *
     * @param row the row to add
     */
    void add(DataStructureNode row);

    /**
     * Creates a tuple acceptor that adds all accepted tuples to the table.
     */
    DataStructureNodeAcceptor<DataStructureNode> acceptor();

    /**
     * Returns the current amount of rows added to the table.
     */
    int getCurrentRows();
}
