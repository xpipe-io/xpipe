package io.xpipe.extension.util;

import io.xpipe.core.source.DataSource;
import io.xpipe.core.source.DataSourceConnection;
import io.xpipe.core.source.DataSourceType;
import io.xpipe.core.source.TextWriteConnection;

public class AppendingTextWriteConnection extends AppendingWriteConnection implements TextWriteConnection {

    public AppendingTextWriteConnection(
            DataSource<?> source, DataSourceConnection connection
    ) {
        super(DataSourceType.TEXT, source, connection);
    }

    @Override
    public void writeLine(String line) throws Exception {
        ((TextWriteConnection) connection).writeLine(line);
    }
}
