package io.xpipe.core.source;

import io.xpipe.core.impl.PreservingTextWriteConnection;
import io.xpipe.core.store.DataStore;
import lombok.experimental.SuperBuilder;

import java.util.concurrent.atomic.AtomicInteger;

@SuperBuilder
public abstract class TextDataSource<DS extends DataStore> extends DataSource<DS> {

    private static final int MAX_LINE_READ = 1000;

    @Override
    public final DataSourceInfo determineInfo() throws Exception {
        if (!getStore().canOpen()) {
            return new DataSourceInfo.Text(-1, -1);
        }

        try (var con = openReadConnection()) {
            AtomicInteger lineCount = new AtomicInteger();
            AtomicInteger charCount = new AtomicInteger();
            con.lines().limit(MAX_LINE_READ).forEach(s -> {
                lineCount.getAndIncrement();
                charCount.addAndGet(s.length());
            });
            boolean limitHit = lineCount.get() == MAX_LINE_READ;
            return new DataSourceInfo.Text(limitHit ? -1 : charCount.get(), limitHit ? -1 : lineCount.get());
        }
    }

    @Override
    public final TextReadConnection openReadConnection() throws Exception {
        var con = newReadConnection();
        con.init();
        return con;
    }

    @Override
    public final TextWriteConnection openWriteConnection(WriteMode mode) throws Exception {
        var con = newWriteConnection(mode);
        con.init();
        return con;
    }

    protected  TextWriteConnection newWriteConnection(WriteMode mode) {
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
