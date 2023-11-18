package io.xpipe.core.process;

import java.io.BufferedInputStream;
import java.io.InputStream;

public class BufferedProcessInputStream extends BufferedInputStream {

    public BufferedProcessInputStream(InputStream in, int size) {
        super(in, size);
    }

    public int bufferedAvailable() {
        return count - pos;
    }
}
