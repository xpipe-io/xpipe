package io.xpipe.api.impl;

import io.xpipe.api.DataRaw;
import io.xpipe.core.source.*;

import java.io.InputStream;

public class DataRawImpl extends DataSourceImpl implements DataRaw {

    private final DataSourceInfo.Raw info;

    public DataRawImpl(DataSourceId sourceId, DataSourceConfigInstance sourceConfig, DataSourceInfo.Raw info) {
        super(sourceId, sourceConfig);
        this.info = info;
    }

    @Override
    public DataSourceInfo.Raw getInfo() {
        return info;
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
