package io.xpipe.ext.json;

import io.xpipe.core.source.RawWriteConnection;
import io.xpipe.core.store.StreamDataStore;

import java.io.OutputStream;

public class MyRawFileWriteConnection implements RawWriteConnection {

    private final StreamDataStore store;
    private OutputStream outputStream;

    public MyRawFileWriteConnection(StreamDataStore store) {
        this.store = store;
    }

    @Override
    public void init() throws Exception {
        if (outputStream != null) {
            throw new IllegalStateException("Already initialized");
        }

        outputStream = store.openOutput();
    }

    @Override
    public void close() throws Exception {
        if (outputStream == null) {
            throw new IllegalStateException("Not initialized");
        }

        outputStream.close();
    }

    @Override
    public void write(byte[] bytes) throws Exception {
        if (outputStream == null) {
            throw new IllegalStateException("Not initialized");
        }

        outputStream.write(bytes);
    }
}
