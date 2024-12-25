package io.xpipe.core.process;

import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class LocalProcessOutputStream extends FilterOutputStream {

    protected LocalProcessOutputStream(OutputStream out) {
        super(out);
    }

    public abstract boolean isClosed();
}
