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

    void add(TupleNode row);

    DataStructureNodeAcceptor<TupleNode> acceptor();

    int getCurrentRows();
}
