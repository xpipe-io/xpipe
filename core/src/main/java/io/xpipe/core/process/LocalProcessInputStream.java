package io.xpipe.core.process;

import java.io.FilterInputStream;
import java.io.InputStream;

public abstract class LocalProcessInputStream extends FilterInputStream {

    protected LocalProcessInputStream(InputStream in) {
        super(in);
    }

    public abstract boolean bufferedAvailable();

    public abstract boolean isClosed();
}
