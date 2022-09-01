package io.xpipe.extension.util;

import io.xpipe.core.source.DataSource;
import io.xpipe.core.source.DataSourceConnection;
import io.xpipe.core.source.DataSourceType;
import io.xpipe.core.store.FileStore;
import io.xpipe.extension.DataSourceProviders;

import java.nio.file.Files;

public class AppendingWriteConnection implements DataSourceConnection {

    private final DataSourceType type;
    private final DataSource<?> source;
    protected final DataSourceConnection connection;

    public AppendingWriteConnection(DataSourceType type, DataSource<?> source, DataSourceConnection connection) {
        this.type = type;
        this.source = source;
        this.connection = connection;
    }

    public void init() throws Exception {
        var temp = Files.createTempFile(null, null);
        var nativeStore = FileStore.local(temp);
        var nativeSource = DataSourceProviders.getNativeDataSourceDescriptorForType(type).createDefaultSource(nativeStore);
        if (source.getStore().canOpen()) {
            try (var in = source.openReadConnection(); var out = nativeSource.openWriteConnection()) {
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
