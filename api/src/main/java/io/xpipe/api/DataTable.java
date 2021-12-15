package io.xpipe.api;

import io.xpipe.api.impl.DataTableImpl;
import io.xpipe.core.data.node.ArrayNode;
import io.xpipe.core.data.node.SimpleTupleNode;
import io.xpipe.core.data.type.DataType;
import io.xpipe.core.source.DataSourceId;

import java.util.OptionalInt;

public interface DataTable extends Iterable<SimpleTupleNode> {

    static DataTable get(String s) {
        return DataTableImpl.get(s);
    }

    DataSourceId getId();

    int getRowCount();

    OptionalInt getRowCountIfPresent();

    DataType getDataType();

    ArrayNode readAll();

    ArrayNode read(int maxRows);
}
