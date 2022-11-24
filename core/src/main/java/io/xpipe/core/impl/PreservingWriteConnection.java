package io.xpipe.core.impl;

import io.xpipe.core.source.DataSource;
import io.xpipe.core.source.DataSourceConnection;
import io.xpipe.core.source.DataSourceType;
import io.xpipe.core.source.WriteMode;

import java.nio.file.Files;

public class PreservingWriteConnection implements DataSourceConnection {

    protected final DataSourceConnection connection;
    private final DataSourceType type;
    private final DataSource<?> source;
    private final boolean append;

    public PreservingWriteConnection(
            DataSourceType type, DataSource<?> source, boolean append, DataSourceConnection connection) {
        this.type = type;
        this.source = source;
        this.append = append;
        this.connection = connection;
    }

    public void init() throws Exception {
        var temp = Files.createTempFile(null, null);
        var nativeStore = FileStore.local(temp);
        var nativeSource = DataSource.createInternalDataSource(type, nativeStore);
        if (source.getStore().canOpen()) {
            try (var in = source.openReadConnection();
                    var out = nativeSource.openWriteConnection(WriteMode.REPLACE)) {
                in.forward(out);
            }
            ;
        }

        connection.init();
        if (source.getStore().canOpen()) {

            try (var in = nativeSource.openReadConnection()) {
                in.forward(connection);
            }
        }
    }

    public void close() throws Exception {
        connection.close();
    }
}
