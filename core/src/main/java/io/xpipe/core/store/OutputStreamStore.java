package io.xpipe.core.store;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class OutputStreamStore implements StreamDataStore {

    private final OutputStream out;

    public OutputStreamStore(OutputStream out) {
        this.out = out;
    }

    @Override
    public InputStream openInput() throws Exception {
        throw new UnsupportedOperationException("No input available");
    }

    @Override
    public OutputStream openOutput() throws Exception {
        return new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                out.write(b);
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                out.write(b, off, len);
            }

            @Override
            public void write(byte[] b) throws IOException {
                out.write(b);
            }

            @Override
            public void flush() throws IOException {
                out.flush();
            }
        };
    }

    @Override
    public boolean exists() {
        return true;
    }
}
