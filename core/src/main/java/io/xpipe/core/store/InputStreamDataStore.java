package io.xpipe.core.store;

import java.io.InputStream;
import java.io.OutputStream;

public abstract class InputStreamDataStore implements StreamDataStore {

    private final InputStream in;

    public InputStreamDataStore(InputStream in) {
        this.in = in;
    }

    @Override
    public InputStream openInput() throws Exception {
        return in;
    }

    @Override
    public OutputStream openOutput() throws Exception {
        throw new UnsupportedOperationException();
    }
}
