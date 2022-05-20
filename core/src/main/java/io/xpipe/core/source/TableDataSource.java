package io.xpipe.core.source;

import io.xpipe.core.store.DataStore;

public abstract class TableDataSource<DS extends DataStore> extends DataSource<DS> {

    public TableDataSource(DS store) {
        super(store);
    }

    @Override
    public final DataSourceInfo determineInfo() throws Exception {
        try (var con = openReadConnection()) {
            var dataType = con.getDataType();
            var rowCount = con.getRowCount();
            return new DataSourceInfo.Table(dataType, rowCount);
        }
    }

    public final TableReadConnection openReadConnection() throws Exception {
        var con = newReadConnection();
        con.init();
        return con;
    }

    public final TableWriteConnection openWriteConnection() throws Exception {
        var con = newWriteConnection();
        con.init();
        return con;
    }

    protected abstract TableWriteConnection newWriteConnection();

    protected abstract TableReadConnection newReadConnection();
}
