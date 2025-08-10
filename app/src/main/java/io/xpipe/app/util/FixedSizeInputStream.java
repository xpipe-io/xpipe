package io.xpipe.app.util;

import java.io.IOException;
import java.io.InputStream;

public class FixedSizeInputStream extends SimpleFilterInputStream {

    private final long size;
    private long count;

    public FixedSizeInputStream(InputStream in, long size) {
        super(in);
        this.size = size;
    }

    @Override
    public int read() throws IOException {
        if (count >= size) {
            return -1;
        }

        var read = in.read();
        count++;
        if (read == -1) {
            return 0;
        } else {
            return read;
        }
    }

    @Override
    public int available() {
        return (int) (size - count);
    }
}
