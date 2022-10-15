package io.xpipe.core.impl;

import io.xpipe.core.source.DataSource;
import io.xpipe.core.source.DataSourceConnection;
import io.xpipe.core.source.DataSourceType;
import io.xpipe.core.source.TextWriteConnection;

public class PreservingTextWriteConnection extends PreservingWriteConnection implements TextWriteConnection {

    public PreservingTextWriteConnection(DataSource<?> source, DataSourceConnection connection, boolean append) {
        super(DataSourceType.TEXT, source, append, connection);
    }

    @Override
    public void writeLine(String line) throws Exception {
        ((TextWriteConnection) connection).writeLine(line);
    }
}
