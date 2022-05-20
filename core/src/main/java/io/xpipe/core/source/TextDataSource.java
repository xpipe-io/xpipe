package io.xpipe.core.source;

import io.xpipe.core.store.DataStore;

public abstract class TextDataSource<DS extends DataStore> extends DataSource<DS> {

    private static final int MAX_LINE_READ = 1000;

    public TextDataSource(DS store) {
        super(store);
    }

    @Override
    public final DataSourceInfo determineInfo() throws Exception {
        try (var con = openReadConnection()) {
            int count = (int) con.lines().limit(MAX_LINE_READ).count();
            int usedCount = count == MAX_LINE_READ ? -1 : count;
            return new DataSourceInfo.Text(usedCount);
        }
    }

    @Override
    public final TextReadConnection openReadConnection() throws Exception {
        var con = newReadConnection();
        con.init();
        return con;
    }

    @Override
    public final TextWriteConnection openWriteConnection() throws Exception {
        var con = newWriteConnection();
        con.init();
        return con;
    }

    protected abstract TextWriteConnection newWriteConnection();

    protected abstract TextReadConnection newReadConnection();
}
