package io.xpipe.core.source;

import io.xpipe.core.impl.PreservingTextWriteConnection;
import io.xpipe.core.store.DataStore;
import lombok.experimental.SuperBuilder;

@SuperBuilder
public abstract class TextDataSource<DS extends DataStore> extends DataSource<DS> {

    private static final int MAX_LINE_READ = 1000;

    @Override
    public DataSourceType getType() {
        return DataSourceType.TEXT;
    }

    @Override
    public final TextReadConnection openReadConnection() {
        if (!isComplete()) {
            throw new UnsupportedOperationException();
        }

        return newReadConnection();
    }

    @Override
    public final TextWriteConnection openWriteConnection(WriteMode mode) {
        var con = newWriteConnection(mode);
        if (con == null) {
            throw new UnsupportedOperationException(mode.getId());
        }

        return con;
    }

    protected TextWriteConnection newWriteConnection(WriteMode mode) {
        if (mode.equals(WriteMode.PREPEND)) {
            return new PreservingTextWriteConnection(this, newWriteConnection(WriteMode.REPLACE), false);
        }

        if (mode.equals(WriteMode.APPEND)) {
            return new PreservingTextWriteConnection(this, newWriteConnection(WriteMode.REPLACE), true);
        }

        return null;
    }

    protected abstract TextReadConnection newReadConnection();
}
