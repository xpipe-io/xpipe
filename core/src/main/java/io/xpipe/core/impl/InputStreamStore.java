package io.xpipe.core.impl;

import io.xpipe.core.store.DataFlow;
import io.xpipe.core.store.StreamDataStore;

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
    public InputStream openInput() {
        return in;
    }

    @Override
    public DataFlow getFlow() {
        return DataFlow.INPUT;
    }

    @Override
    public boolean canOpen() {
        return true;
    }
}