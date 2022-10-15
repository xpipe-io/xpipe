package io.xpipe.core.source;

import io.xpipe.core.charsetter.StreamCharset;
import io.xpipe.core.store.StreamDataStore;

import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class StreamWriteConnection implements DataSourceConnection {

    protected final StreamDataStore store;
    private final StreamCharset charset;
    protected OutputStream outputStream;
    protected OutputStreamWriter writer;

    public StreamWriteConnection(StreamDataStore store, StreamCharset charset) {
        this.store = store;
        this.charset = charset;
    }

    @Override
    public void init() throws Exception {
        if (outputStream != null) {
            throw new IllegalStateException("Already initialized");
        }

        outputStream = store.openOutput();
        if (charset != null) {
            if (charset.hasByteOrderMark()) {
                outputStream.write(charset.getByteOrderMark());
            }
            writer = new OutputStreamWriter(outputStream, charset.getCharset());
        }
    }

    @Override
    public void close() throws Exception {
        if (outputStream == null) {
            throw new IllegalStateException("Not initialized");
        }

        if (writer != null) {
            writer.close();
        }

        outputStream.close();
    }
}
