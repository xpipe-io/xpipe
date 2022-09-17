package io.xpipe.core.store;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.io.IOException;
import java.io.OutputStream;

@EqualsAndHashCode
@Value
@JsonTypeName("stdout")
public class StdoutDataStore implements StreamDataStore {

    @Override
    public boolean canOpen() throws Exception {
        return false;
    }

    @Override
    public boolean isContentExclusivelyAccessible() {
        return true;
    }

    @Override
    public OutputStream openOutput() throws Exception {
        // Create an output stream that will write to standard out but will not close it
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
}
