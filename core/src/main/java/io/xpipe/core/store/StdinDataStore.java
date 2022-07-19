package io.xpipe.core.store;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@EqualsAndHashCode
@Value
@JsonTypeName("stdin")
public class StdinDataStore implements StreamDataStore {

    @Override
    public InputStream openInput() throws Exception {
        var in = System.in;
        // Prevent closing the standard in when the returned input stream is closed
        return new InputStream() {
            @Override
            public int read() throws IOException {
                return in.read();
            }

            @Override
            public int read(byte[] b) throws IOException {
                return in.read(b);
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                return in.read(b, off, len);
            }

            @Override
            public byte[] readAllBytes() throws IOException {
                return in.readAllBytes();
            }

            @Override
            public byte[] readNBytes(int len) throws IOException {
                return in.readNBytes(len);
            }

            @Override
            public int readNBytes(byte[] b, int off, int len) throws IOException {
                return in.readNBytes(b, off, len);
            }

            @Override
            public long skip(long n) throws IOException {
                return in.skip(n);
            }

            @Override
            public void skipNBytes(long n) throws IOException {
                in.skipNBytes(n);
            }

            @Override
            public int available() throws IOException {
                return in.available();
            }

            @Override
            public void close() throws IOException {
            }

            @Override
            public synchronized void mark(int readlimit) {
                in.mark(readlimit);
            }

            @Override
            public synchronized void reset() throws IOException {
                in.reset();
            }

            @Override
            public boolean markSupported() {
                return in.markSupported();
            }

            @Override
            public long transferTo(OutputStream out) throws IOException {
                return in.transferTo(out);
            }
        };
    }
}
