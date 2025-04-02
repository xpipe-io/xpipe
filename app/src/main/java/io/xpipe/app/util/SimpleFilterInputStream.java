package io.xpipe.app.util;

import lombok.NonNull;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public abstract class SimpleFilterInputStream extends FilterInputStream {

    protected SimpleFilterInputStream(InputStream in) {
        super(in);
    }

    @Override
    public abstract int read() throws IOException;

    @Override
    public int read(byte @NonNull [] b, int off, int len) throws IOException {
        for (int i = off; i < off + len; i++) {
            var r = read();
            if (r == -1) {
                return i - off == 0 ? -1 : i - off;
            }

            b[i] = (byte) r;
        }
        return len;
    }
}
