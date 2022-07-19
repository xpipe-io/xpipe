package io.xpipe.core.store;

import java.io.InputStream;

/**
 * A data store that is only represented by an InputStream.
 * This can be useful for development.
 */
public class InputStreamDataStore implements StreamDataStore {

    private final InputStream in;

    public InputStreamDataStore(InputStream in) {
        this.in = in;
    }

    @Override
    public InputStream openInput() throws Exception {
        return in;
    }

    @Override
    public boolean canOpen() {
        return true;
    }
}
