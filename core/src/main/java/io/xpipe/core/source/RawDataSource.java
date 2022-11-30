package io.xpipe.core.source;

import io.xpipe.core.store.DataStore;
import lombok.experimental.SuperBuilder;

@SuperBuilder
public abstract class RawDataSource<DS extends DataStore> extends DataSource<DS> {

    private static final int MAX_BYTES_READ = 100000;

    @Override
    public DataSourceType getType() {
        return DataSourceType.RAW;
    }

    @Override
    public final RawReadConnection openReadConnection() throws Exception {
        if (!isComplete()) {
            throw new UnsupportedOperationException();
        }

        return newReadConnection();
    }

    @Override
    public final RawWriteConnection openWriteConnection(WriteMode mode) throws Exception {
        var con = newWriteConnection(mode);
        if (con == null) {
            throw new UnsupportedOperationException(mode.getId());
        }

        return con;
    }

    protected abstract RawWriteConnection newWriteConnection(WriteMode mode);

    protected abstract RawReadConnection newReadConnection();
}
