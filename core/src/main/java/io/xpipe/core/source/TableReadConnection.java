package io.xpipe.core.source;


import io.xpipe.core.data.node.DataStructureNodeAcceptor;
import io.xpipe.core.data.node.ArrayNode;
import io.xpipe.core.data.node.TupleNode;
import io.xpipe.core.data.type.TupleType;
import io.xpipe.core.data.typed.TypedDataStreamWriter;

import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A connection for sequentially reading the data of a table data source.
 */
public interface TableReadConnection extends DataSourceReadConnection {

    /**
     * Returns the data type of the table data.
     */
    TupleType getDataType() throws Exception;

    /**
     * Returns the amount of rows to be read or -1 if the amount is unknown.
     */
    int getRowCount() throws Exception;

    /**
     * Consumes the table rows until the acceptor returns false.
     */
    void withRows(DataStructureNodeAcceptor<TupleNode> lineAcceptor) throws Exception;

    /**
     * Reads multiple rows in bulk.
     */
    ArrayNode readRows(int maxLines) throws Exception;

    /**
     * Writes the rows to an OutputStream in the X-Pipe binary format.
     */
    default void forwardRows(OutputStream out, int maxLines) throws Exception {
        if (maxLines == 0) {
            return;
        }

        var dataType = getDataType();
        AtomicInteger rowCounter = new AtomicInteger();
        withRows(l -> {
            TypedDataStreamWriter.writeStructure(out, l, dataType);
            rowCounter.getAndIncrement();
            return rowCounter.get() != maxLines;
        });
    }

    default void forward(DataSourceConnection con) throws Exception {
        try (var tCon = (TableWriteConnection) con) {
            tCon.init();
            withRows(tCon.writeLinesAcceptor());
        }
    }
}
