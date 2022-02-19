package io.xpipe.api;

import io.xpipe.core.data.node.ArrayNode;
import io.xpipe.core.data.node.TupleNode;
import io.xpipe.core.data.type.TupleType;

import java.util.OptionalInt;
import java.util.stream.Stream;

public interface DataTable extends Iterable<TupleNode>, DataSource {

    /**
     * @see DataSource#supplySource()
     */
    static DataTable supplySource() {
        return null;
    }

    Stream<TupleNode> stream();

    int getRowCount();

    OptionalInt getRowCountIfPresent();

    TupleType getDataType();

    ArrayNode readAll();

    ArrayNode read(int maxRows);
}
