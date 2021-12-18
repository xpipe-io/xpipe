package io.xpipe.core.source;

import io.xpipe.core.store.DataStore;

public abstract class TableDataSourceDescriptor<DS extends DataStore> implements DataSourceDescriptor<DS> {

    public abstract TableDataWriteConnection newWriteConnection(DS store);

    public abstract TableDataReadConnection newReadConnection(DS store);

    @Override
    public DataSourceType getType() {
        return DataSourceType.TABLE;
    }
}
