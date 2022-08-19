package io.xpipe.core.source;

import java.io.OutputStream;

public interface RawReadConnection extends DataSourceReadConnection {

    byte[] readBytes(int max) throws Exception;

    int BUFFER_SIZE = 8192;

    default void forwardBytes(OutputStream out, int maxBytes) throws Exception {
        if (maxBytes == 0) {
            return;
        }

        out.write(readBytes(maxBytes));
    }

    default void forward(DataSourceConnection con) throws Exception {
        try (var tCon = (RawWriteConnection) con) {
            byte[] b;
            while ((b = readBytes(BUFFER_SIZE)).length > 0) {
                tCon.write(b);
            }
        }
    }
}
