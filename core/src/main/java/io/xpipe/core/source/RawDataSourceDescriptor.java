package io.xpipe.core.source;

import io.xpipe.core.store.DataStore;

public abstract class RawDataSourceDescriptor <DS extends DataStore> implements DataSourceDescriptor<DS>  {

    private static final int MAX_BYTES_READ = 100000;

    @Override
    public DataSourceInfo determineInfo(DS store) throws Exception {
        try (var con = openReadConnection(store)) {
            var b = con.readBytes(MAX_BYTES_READ);
            int usedCount = b.length == MAX_BYTES_READ ? -1 : b.length;
            return new DataSourceInfo.Raw(usedCount);
        }
    }

    @Override
    public RawReadConnection openReadConnection(DS store) throws Exception {
        var con = newReadConnection(store);
        con.init();
        return con;
    }

    @Override
    public RawWriteConnection openWriteConnection(DS store) throws Exception {
        var con = newWriteConnection(store);
        con.init();
        return con;
    }

    protected abstract RawWriteConnection newWriteConnection(DS store);

    protected abstract RawReadConnection newReadConnection(DS store);
}
