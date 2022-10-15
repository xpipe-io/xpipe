package io.xpipe.core.source;

import io.xpipe.core.charsetter.Charsetter;
import io.xpipe.core.charsetter.StreamCharset;
import io.xpipe.core.store.StreamDataStore;

import java.io.InputStream;
import java.io.Reader;

public abstract class StreamReadConnection implements DataSourceReadConnection {

    protected final StreamDataStore store;
    private final StreamCharset charset;
    protected InputStream inputStream;
    protected Reader reader;

    public StreamReadConnection(StreamDataStore store, StreamCharset charset) {
        this.store = store;
        this.charset = charset;
    }

    @Override
    public void init() throws Exception {
        if (inputStream != null) {
            throw new IllegalStateException("Already initialized");
        }

        inputStream = store.openInput();
        if (charset != null) {
            reader = Charsetter.get().reader(inputStream, charset);
        }
    }

    @Override
    public void close() throws Exception {
        if (inputStream == null) {
            throw new IllegalStateException("Not initialized");
        }

        inputStream.close();
    }
}
