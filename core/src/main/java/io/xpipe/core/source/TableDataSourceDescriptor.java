package io.xpipe.core.source;

import io.xpipe.core.store.DataStore;

public abstract class TableDataSourceDescriptor<DS extends DataStore> implements DataSourceDescriptor<DS> {

    public final TableDataReadConnection openReadConnection(DS store) throws Exception {
        var con = newReadConnection(store);
        con.init();
        return con;
    }

    public final TableDataWriteConnection openWriteConnection(DS store) throws Exception {
        var con = newWriteConnection(store);
        con.init();
        return con;
    }

    protected abstract TableDataWriteConnection newWriteConnection(DS store);

    protected abstract TableDataReadConnection newReadConnection(DS store);

    @Override
    public DataSourceType getType() {
        return DataSourceType.TABLE;
    }
}
