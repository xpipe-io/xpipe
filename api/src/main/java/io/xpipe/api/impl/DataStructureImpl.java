package io.xpipe.api.impl;

import io.xpipe.api.DataStructure;
import io.xpipe.core.data.node.DataStructureNode;
import io.xpipe.core.source.*;

public class DataStructureImpl extends DataSourceImpl implements DataStructure {

    private final DataSourceInfo.Structure info;

    public DataStructureImpl(DataSourceId sourceId, DataSourceConfigInstance sourceConfig, DataSourceInfo.Structure info) {
        super(sourceId, sourceConfig);
        this.info = info;
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
    public DataSourceInfo.Structure getInfo() {
        return info;
    }

    @Override
    public DataStructureNode read() {
        return null;
    }
}
