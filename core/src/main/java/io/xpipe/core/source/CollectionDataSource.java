package io.xpipe.core.source;

import io.xpipe.core.store.DataStore;
import lombok.experimental.SuperBuilder;

@SuperBuilder
public abstract class CollectionDataSource<DS extends DataStore> extends DataSource<DS> {

    @Override
    public DataSourceType getType() {
        return DataSourceType.COLLECTION;
    }

    public final CollectionReadConnection openReadConnection() throws Exception {
        if (!isComplete()) {
            throw new UnsupportedOperationException();
        }

        return newReadConnection();
    }

    public final CollectionWriteConnection openWriteConnection(WriteMode mode) throws Exception {
        var con = newWriteConnection(mode);
        if (con == null) {
            throw new UnsupportedOperationException(mode.getId());
        }

        return con;
    }

    protected abstract CollectionWriteConnection newWriteConnection(WriteMode mode);

    protected abstract CollectionReadConnection newReadConnection() throws Exception;
}
