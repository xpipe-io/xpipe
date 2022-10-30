package io.xpipe.core.impl;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.source.RawDataSource;
import io.xpipe.core.source.RawReadConnection;
import io.xpipe.core.source.RawWriteConnection;
import io.xpipe.core.source.WriteMode;
import io.xpipe.core.store.StreamDataStore;
import lombok.experimental.SuperBuilder;

import java.io.InputStream;
import java.io.OutputStream;

@JsonTypeName("binary")
@SuperBuilder
public class BinarySource extends RawDataSource<StreamDataStore> {

    @Override
    protected RawWriteConnection newWriteConnection(WriteMode mode) {
        return new RawWriteConnection() {

            private OutputStream out;

            @Override
            public void init() throws Exception {
                out = getStore().openOutput();
            }

            @Override
            public void close() throws Exception {
                out.close();
            }

            @Override
            public void write(byte[] bytes) throws Exception {
                out.write(bytes);
            }
        };
    }

    @Override
    protected RawReadConnection newReadConnection() {
        return new RawReadConnection() {

            @Override
            public boolean canRead() throws Exception {
                return getStore().canOpen();
            }

            private InputStream inputStream;

            @Override
            public void init() throws Exception {
                if (inputStream != null) {
                    throw new IllegalStateException("Already initialized");
                }

                inputStream = getStore().openInput();
            }

            @Override
            public void close() throws Exception {
                inputStream.close();
            }

            @Override
            public byte[] readBytes(int max) throws Exception {
                return inputStream.readNBytes(max);
            }
        };
    }
}
