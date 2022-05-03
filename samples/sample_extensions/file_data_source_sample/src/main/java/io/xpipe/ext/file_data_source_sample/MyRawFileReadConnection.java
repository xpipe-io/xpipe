package io.xpipe.ext.json;

import io.xpipe.core.source.RawReadConnection;
import io.xpipe.core.store.StreamDataStore;

import java.io.InputStream;

public class MyRawFileReadConnection implements RawReadConnection {

    private InputStream inputStream;
    private final StreamDataStore store;

    public MyRawFileReadConnection(StreamDataStore store) {
        this.store = store;
    }

    @Override
    public void init() throws Exception {
        if (inputStream != null) {
            throw new IllegalStateException("Already initialized");
        }

        inputStream = store.openInput();
    }

    @Override
    public void close() throws Exception {
        if (inputStream == null) {
            throw new IllegalStateException("Not initialized");
        }

        inputStream.close();
    }

    @Override
    public byte[] readBytes(int max) throws Exception {
        if (inputStream == null) {
            throw new IllegalStateException("Not initialized");
        }

        return inputStream.readNBytes(max);
    }
}
