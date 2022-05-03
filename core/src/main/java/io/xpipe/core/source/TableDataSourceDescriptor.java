package io.xpipe.core.source;

import io.xpipe.core.store.DataStore;

public abstract class TableDataSourceDescriptor<DS extends DataStore> implements DataSourceDescriptor<DS> {

    public final TableReadConnection openReadConnection(DS store) throws Exception {
        var con = newReadConnection(store);
        con.init();
        return con;
    }

    public final TableWriteConnection openWriteConnection(DS store) throws Exception {
        var con = newWriteConnection(store);
        con.init();
        return con;
    }

    protected abstract TableWriteConnection newWriteConnection(DS store);

    protected abstract TableReadConnection newReadConnection(DS store);
}
