package io.xpipe.core.store;

import java.io.InputStream;
import java.io.OutputStream;

public class OutputStreamStore implements StreamDataStore {

    private final OutputStream out;

    public OutputStreamStore(OutputStream out) {
        this.out = out;
    }

    @Override
    public boolean isContentExclusivelyAccessible() {
        return true;
    }

    @Override
    public DataFlow getFlow() {
        return DataFlow.OUTPUT;
    }

    @Override
    public InputStream openInput() {
        throw new UnsupportedOperationException("No input available");
    }

    @Override
    public OutputStream openOutput() {
        return out;
    }

    @Override
    public boolean canOpen() {
        return false;
    }
}
