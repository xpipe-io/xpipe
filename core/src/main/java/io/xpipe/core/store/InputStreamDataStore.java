package io.xpipe.core.store;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class InputStreamDataStore implements StreamDataStore {

    private final InputStream in;
    private BufferedInputStream bufferedInputStream;
    private boolean opened = false;

    public InputStreamDataStore(InputStream in) {
        this.in = in;
    }

    @Override
    public InputStream openInput() throws Exception {
        if (opened) {
            return bufferedInputStream;
        }

        opened = true;
        bufferedInputStream = new BufferedInputStream(in);
        return bufferedInputStream;
    }

    @Override
    public OutputStream openOutput() throws Exception {
        throw new UnsupportedOperationException("No output available");
    }
}
