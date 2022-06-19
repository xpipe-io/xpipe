package io.xpipe.core.store;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A data store that is only represented by an InputStream.
 * One common use case of this class are piped inputs.
 *
 * As the data in a pipe can only be read once, this implementation
 * internally uses a BufferedInputStream to support mark/rest.
 */
public class InputStreamDataStore implements StreamDataStore {

    private final InputStream in;
    private BufferedInputStream bufferedInputStream;

    public InputStreamDataStore(InputStream in) {
        this.in = in;
    }

    @Override
    public InputStream openInput() throws Exception {
        if (bufferedInputStream != null) {
            bufferedInputStream.reset();
            return bufferedInputStream;
        }

        bufferedInputStream = new BufferedInputStream(in);
        bufferedInputStream.mark(Integer.MAX_VALUE);
        return new InputStream() {
            @Override
            public int read() throws IOException {
                return bufferedInputStream.read();
            }

            @Override
            public int read(byte[] b) throws IOException {
                return bufferedInputStream.read(b);
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                return bufferedInputStream.read(b, off, len);
            }

            @Override
            public byte[] readAllBytes() throws IOException {
                return bufferedInputStream.readAllBytes();
            }

            @Override
            public byte[] readNBytes(int len) throws IOException {
                return bufferedInputStream.readNBytes(len);
            }

            @Override
            public int readNBytes(byte[] b, int off, int len) throws IOException {
                return bufferedInputStream.readNBytes(b, off, len);
            }

            @Override
            public long skip(long n) throws IOException {
                return bufferedInputStream.skip(n);
            }

            @Override
            public void skipNBytes(long n) throws IOException {
                bufferedInputStream.skipNBytes(n);
            }

            @Override
            public int available() throws IOException {
                return bufferedInputStream.available();
            }

            @Override
            public void close() throws IOException {
                reset();
            }

            @Override
            public synchronized void mark(int readlimit) {
                bufferedInputStream.mark(readlimit);
            }

            @Override
            public synchronized void reset() throws IOException {
                bufferedInputStream.reset();
            }

            @Override
            public boolean markSupported() {
                return bufferedInputStream.markSupported();
            }

            @Override
            public long transferTo(OutputStream out) throws IOException {
                return bufferedInputStream.transferTo(out);
            }
        };
    }

    @Override
    public OutputStream openOutput() throws Exception {
        throw new UnsupportedOperationException("No output available");
    }

    @Override
    public boolean canOpen() {
        return true;
    }
}
