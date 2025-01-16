package io.xpipe.core.process;

import java.io.FilterOutputStream;
import java.io.OutputStream;

public abstract class LocalProcessOutputStream extends FilterOutputStream {

    protected LocalProcessOutputStream(OutputStream out) {
        super(out);
    }

    public abstract boolean isClosed();
}
