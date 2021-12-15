package io.xpipe.core.source;

import io.xpipe.core.store.DataStore;

public abstract class TableDataSourceDescriptor<DS extends DataStore> implements DataSourceDescriptor<DS> {

    public abstract TableDataWriteConnection openWriteConnection(DS store);

    public abstract TableDataReadConnection openConnection(DS store);

    @Override
    public DataSourceType getType() {
        return DataSourceType.TABLE;
    }
}
