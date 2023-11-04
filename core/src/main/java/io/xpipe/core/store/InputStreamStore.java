package io.xpipe.core.store;

import java.io.InputStream;

/**
 * A data store that is only represented by an InputStream.
 */
public class InputStreamStore implements StreamDataStore {

    private final InputStream in;

    public InputStreamStore(InputStream in) {
        this.in = in;
    }

    @Override
    public DataFlow getFlow() {
        return DataFlow.INPUT;
    }

    @Override
    public boolean canOpen() {
        return true;
    }

    @Override
    public InputStream openInput() {
        return in;
    }
}