package io.xpipe.core.source;

import io.xpipe.core.store.DataStore;

public abstract class TextDataSourceDescriptor<DS extends DataStore> implements DataSourceDescriptor<DS>  {

    private static final int MAX_LINE_READ = 1000;

    @Override
    public DataSourceInfo determineInfo(DS store) throws Exception {
        try (var con = openReadConnection(store)) {
            int count = (int) con.lines().limit(MAX_LINE_READ).count();
            int usedCount = count == MAX_LINE_READ ? -1 : count;
            return new DataSourceInfo.Text(usedCount);
        }
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
