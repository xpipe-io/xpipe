package io.xpipe.api.impl;

import io.xpipe.api.DataSourceConfig;
import io.xpipe.api.DataStructure;
import io.xpipe.core.data.node.DataStructureNode;
import io.xpipe.core.source.DataSourceId;
import io.xpipe.core.source.DataSourceType;

public class DataStructureImpl extends DataSourceImpl implements DataStructure {

    DataStructureImpl(
            DataSourceId sourceId, DataSourceConfig sourceConfig, io.xpipe.core.source.DataSource<?> internalSource) {
        super(sourceId, sourceConfig, internalSource);
    }

    @Override
    public DataSourceType getType() {
        return DataSourceType.STRUCTURE;
    }

    @Override
    public DataStructure asStructure() {
        return this;
    }

    @Override
    public DataStructureNode read() {
        return null;
    }
}
