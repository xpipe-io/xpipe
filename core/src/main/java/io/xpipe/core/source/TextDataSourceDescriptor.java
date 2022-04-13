package io.xpipe.core.source;

import io.xpipe.core.store.DataStore;

public abstract class TextDataSourceDescriptor<DS extends DataStore> implements DataSourceDescriptor<DS>  {

    @Override
    public DataSourceType getType() {
        return DataSourceType.TEXT;
    }

    @Override
    public TextReadConnection openReadConnection(DS store) throws Exception {
        var con = newReadConnection(store);
        con.init();
        return con;
    }

    @Override
    public TextWriteConnection openWriteConnection(DS store) throws Exception {
        var con = newWriteConnection(store);
        con.init();
        return con;
    }

    protected abstract TextWriteConnection newWriteConnection(DS store);

    protected abstract TextReadConnection newReadConnection(DS store);
}
