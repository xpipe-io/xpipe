package io.xpipe.core.source;

import io.xpipe.core.impl.PreservingTextWriteConnection;
import io.xpipe.core.store.DataStore;
import lombok.NoArgsConstructor;

import java.util.concurrent.atomic.AtomicInteger;

@NoArgsConstructor
public abstract class TextDataSource<DS extends DataStore> extends DataSource<DS> {

    private static final int MAX_LINE_READ = 1000;

    public TextDataSource(DS store) {
        super(store);
    }

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
    public final TextWriteConnection openWriteConnection() throws Exception {
        var con = newWriteConnection();
        con.init();
        return con;
    }

    @Override
    public final TextWriteConnection openAppendingWriteConnection() throws Exception {
        var con = newAppendingWriteConnection();
        con.init();
        return con;
    }


    @Override
    public final TextWriteConnection openPrependingWriteConnection() throws Exception {
        var con = newPrependingWriteConnection();
        con.init();
        return con;
    }

    protected abstract TextWriteConnection newWriteConnection();

    protected TextWriteConnection newAppendingWriteConnection() {
        return new PreservingTextWriteConnection(this, newWriteConnection(), true);
    }


    protected TextWriteConnection newPrependingWriteConnection() {
        return new PreservingTextWriteConnection(this, newWriteConnection(), false);
    }




    protected abstract TextReadConnection newReadConnection();
}
