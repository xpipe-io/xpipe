package io.xpipe.api.impl;

import io.xpipe.api.DataRaw;
import io.xpipe.api.DataSourceConfig;
import io.xpipe.core.source.DataSourceId;
import io.xpipe.core.source.DataSourceType;

import java.io.InputStream;

public class DataRawImpl extends DataSourceImpl implements DataRaw {

    public DataRawImpl(
            DataSourceId sourceId, DataSourceConfig sourceConfig, io.xpipe.core.source.DataSource<?> internalSource) {
        super(sourceId, sourceConfig, internalSource);
    }

    @Override
    public InputStream open() {
        return null;
    }

    @Override
    public byte[] readAll() {
        return new byte[0];
    }

    @Override
    public byte[] read(int maxBytes) {
        return new byte[0];
    }

    @Override
    public DataSourceType getType() {
        return DataSourceType.RAW;
    }

    @Override
    public DataRaw asRaw() {
        return this;
    }
}
