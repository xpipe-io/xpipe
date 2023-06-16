package io.xpipe.core.source;

import io.xpipe.core.data.type.TupleType;
import io.xpipe.core.impl.PreservingTableWriteConnection;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.util.ProxyProvider;
import io.xpipe.core.util.SimpleProxyFunction;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
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
    public DataSourceType getType() {
        return DataSourceType.TABLE;
    }

    public final TableReadConnection openReadConnection() throws Exception {
        if (!isComplete()) {
            return TableReadConnection.empty();
        }

        var proxy = ProxyProvider.get().getProxy(this);
        if (proxy != null) {
            return ProxyProvider.get().createRemoteReadConnection(this, proxy);
        }

        return newReadConnection();
    }

    public final Optional<TableMapping> createMapping(TupleType inputType) {
        return Optional.ofNullable(new CreateMappingFunction(this, inputType).callAndGet());
    }

    public final TableWriteConnection openWriteConnection(WriteMode mode) throws Exception {
        var con = newWriteConnection(mode);
        if (con == null) {
            throw new UnsupportedOperationException(mode.getId());
        }

        var proxy = ProxyProvider.get().getProxy(this);
        if (proxy != null) {
            return ProxyProvider.get().createRemoteWriteConnection(this, mode, proxy);
        }

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

    @NoArgsConstructor
    private static class CreateMappingFunction extends SimpleProxyFunction<TableMapping> {

        private TableDataSource<?> source;
        private TupleType type;
        private TableMapping mapping;

        public CreateMappingFunction(TableDataSource<?> source, TupleType type) {
            this.source = source;
            this.type = type;
        }

        @SneakyThrows
        public void callLocal() {
            try (TableWriteConnection w = source.openWriteConnection(WriteMode.REPLACE)) {
                w.init();
                mapping = w.createMapping(type).orElse(null);
            }
        }
    }
}
