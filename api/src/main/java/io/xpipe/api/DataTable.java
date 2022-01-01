package io.xpipe.api;

import io.xpipe.core.data.node.ArrayNode;
import io.xpipe.core.data.node.TupleNode;
import io.xpipe.core.data.type.DataType;

import java.util.OptionalInt;
import java.util.stream.Stream;

public interface DataTable extends Iterable<TupleNode>, DataSource {

    Stream<TupleNode> stream();

    int getRowCount();

    OptionalInt getRowCountIfPresent();

    DataType getDataType();

    ArrayNode readAll();

    ArrayNode read(int maxRows);
}
