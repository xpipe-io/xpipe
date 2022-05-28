package io.xpipe.core.store;

import lombok.EqualsAndHashCode;
import lombok.Value;

import java.io.IOException;
import java.io.OutputStream;

@EqualsAndHashCode
@Value
public class StdoutDataStore implements StreamDataStore {

    @Override
    public OutputStream openOutput() throws Exception {
        return new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                System.out.write(b);
            }

            @Override
            public void write(byte[] b) throws IOException {
                System.out.write(b);
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                System.out.write(b, off, len);
            }

            @Override
            public void flush() throws IOException {
                System.out.flush();
            }

            @Override
            public void close() throws IOException {
            }
        };
    }

    @Override
    public boolean exists() {
        return false;
    }
}
