package io.xpipe.core.source;

import io.xpipe.core.store.DataStore;
import lombok.experimental.SuperBuilder;

@SuperBuilder
public abstract class RawDataSource<DS extends DataStore> extends DataSource<DS> {

    private static final int MAX_BYTES_READ = 100000;

    @Override
    public final DataSourceInfo determineInfo() throws Exception {
        try (var con = openReadConnection()) {
            var b = con.readBytes(MAX_BYTES_READ);
            int usedCount = b.length == MAX_BYTES_READ ? -1 : b.length;
            return new DataSourceInfo.Raw(usedCount);
        }
    }

    @Override
    public final RawReadConnection openReadConnection() throws Exception {
        var con = newReadConnection();
        con.init();
        return con;
    }

    @Override
    public final RawWriteConnection openWriteConnection(WriteMode mode) throws Exception {
        var con = newWriteConnection(mode);
        if (con == null) {
            throw new UnsupportedOperationException(mode.getId());
        }

        con.init();
        return con;
    }

    protected abstract RawWriteConnection newWriteConnection(WriteMode mode);

    protected abstract RawReadConnection newReadConnection();
}
