package io.xpipe.api;

import io.xpipe.core.data.node.DataStructureNodeAcceptor;
import io.xpipe.core.data.node.TupleNode;
import io.xpipe.core.source.DataSourceId;

/**
 * An accumulator for table data.
 *
 * This class can be used to construct new table data sources by
 * accumulating the rows using {@link #add(TupleNode)} or {@link #acceptor()} and then calling
 * {@link #finish(DataSourceId)} to complete the construction process and create a new data source.
 */
public interface DataTableAccumulator {

    /**
     * Finishes the construction process and returns the data source reference.
     *
     * @param id the data source id to assign
     */
    DataTable finish(DataSourceId id);

    /**
     * Adds a row to the table.
     *
     * @param row the row to add
     */
    void add(TupleNode row);

    /**
     * Creates a tuple acceptor that adds all accepted tuples to the table.
     */
    DataStructureNodeAcceptor<TupleNode> acceptor();

    /**
     * Returns the current amount of rows added to the table.
     */
    int getCurrentRows();
}
