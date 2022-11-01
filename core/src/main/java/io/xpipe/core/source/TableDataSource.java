package io.xpipe.core.source;

import io.xpipe.core.data.type.TupleType;
import io.xpipe.core.impl.PreservingTableWriteConnection;
import io.xpipe.core.store.DataStore;
import lombok.experimental.SuperBuilder;

import java.util.Optional;

@SuperBuilder
public abstract class TableDataSource<DS extends DataStore> extends DataSource<DS> {

    public Optional<TupleType> determineDataType() throws Exception {
        var readConnection = newReadConnection();
        var canRead = readConnection != null && readConnection.canRead();
        if (canRead) {
            try (var in = readConnection) {
                readConnection.init();
                return Optional.ofNullable(readConnection.getDataType());
            }
        } else {
            return Optional.empty();
        }
    }

    @Override
    public final DataSourceInfo determineInfo() throws Exception {
        if (!getFlow().hasInput()) {
            return new DataSourceInfo.Table(null, -1);
        }

        try (var con = openReadConnection()) {
            var dataType = con.getDataType();
            var rowCount = con.getRowCount();
            return new DataSourceInfo.Table(dataType, rowCount.orElse(-1));
        }
    }

    public final TableReadConnection openReadConnection() throws Exception {
        try {
            checkComplete();
        } catch (Exception e) {
            return TableReadConnection.empty();
        }

        var con = newReadConnection();
        con.init();
        return con;
    }

    public final TableWriteConnection openWriteConnection(WriteMode mode) throws Exception {
        var con = newWriteConnection(mode);
        if (con == null) {
            throw new UnsupportedOperationException(mode.getId());
        }

        con.init();
        return con;
    }

    protected TableWriteConnection newWriteConnection(WriteMode mode) {
        if (mode.equals(WriteMode.PREPEND)) {
            return new PreservingTableWriteConnection(this, newWriteConnection(WriteMode.REPLACE), false);
        }

        if (mode.equals(WriteMode.APPEND)) {
            return new PreservingTableWriteConnection(this, newWriteConnection(WriteMode.REPLACE), true);
        }

        return null;
    }

    protected TableReadConnection newReadConnection() {
        throw new UnsupportedOperationException();
    }
}
