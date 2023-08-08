package io.xpipe.api.impl;

import io.xpipe.api.DataSourceConfig;
import io.xpipe.api.DataTable;
import io.xpipe.core.data.node.ArrayNode;
import io.xpipe.core.data.node.DataStructureNode;
import io.xpipe.core.data.node.TupleNode;
import io.xpipe.core.source.DataStoreId;
import io.xpipe.core.source.DataSourceType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

public class DataTableImpl extends DataSourceImpl implements DataTable {

    DataTableImpl(DataStoreId id, DataSourceConfig sourceConfig, io.xpipe.core.source.DataSource<?> internalSource) {
        super(id, sourceConfig, internalSource);
    }

    @Override
    public DataTable asTable() {
        return this;
    }

    public Stream<TupleNode> stream() {
        return Stream.of();
    }

    @Override
    public DataSourceType getType() {
        return DataSourceType.TABLE;
    }

    @Override
    public ArrayNode readAll() {
        return read(Integer.MAX_VALUE);
    }

    @Override
    public ArrayNode read(int maxRows) {
        List<DataStructureNode> nodes = new ArrayList<>();
        return ArrayNode.of(nodes);
    }

    @Override
    public Iterator<TupleNode> iterator() {
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public TupleNode next() {
                return null;
            }
        };
    }
}
