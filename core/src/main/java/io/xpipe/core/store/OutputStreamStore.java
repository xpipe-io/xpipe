package io.xpipe.core.store;

import java.io.InputStream;
import java.io.OutputStream;

public class OutputStreamStore implements StreamDataStore {

    private final OutputStream out;

    public OutputStreamStore(OutputStream out) {
        this.out = out;
    }

    @Override
    public InputStream openInput() throws Exception {
        throw new UnsupportedOperationException("No input available");
    }

    @Override
    public OutputStream openOutput() throws Exception {
        return out;
    }

    @Override
    public boolean exists() {
        return true;
    }
}
