package io.xpipe.api.impl;

import io.xpipe.api.DataText;
import io.xpipe.core.source.DataSourceConfig;
import io.xpipe.core.source.DataSourceId;
import io.xpipe.core.source.DataSourceInfo;
import io.xpipe.core.source.DataSourceType;

import java.util.Iterator;
import java.util.List;

public class DataTextImpl extends DataSourceImpl implements DataText {

    private final DataSourceInfo.Text info;

    public DataTextImpl(DataSourceId sourceId, DataSourceConfig sourceConfig, DataSourceInfo.Text info) {
        super(sourceId, sourceConfig);
        this.info = info;
    }

    @Override
    public DataSourceType getType() {
        return DataSourceType.TEXT;
    }

    @Override
    public DataText asText() {
        return this;
    }

    @Override
    public DataSourceInfo.Text getInfo() {
        return info;
    }

    @Override
    public List<String> readAllLines() {
        return null;
    }

    @Override
    public List<String> readLines(int maxLines) {
        return null;
    }

    @Override
    public String readAll() {
        return null;
    }

    @Override
    public String read(int maxCharacters) {
        return null;
    }

    @Override
    public Iterator<String> iterator() {
        return null;
    }
}
