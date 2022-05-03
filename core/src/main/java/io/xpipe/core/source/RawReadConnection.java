package io.xpipe.core.source;

public interface RawReadConnection extends DataSourceReadConnection {

    byte[] readBytes(int max) throws Exception;

    int BUFFER_SIZE = 8192;

    default void forward(DataSourceConnection con) throws Exception {
        try (var tCon = (RawWriteConnection) con) {
            tCon.init();
            byte[] b;
            while ((b = readBytes(BUFFER_SIZE)).length > 0) {
                tCon.write(b);
            }
        }
    }
}
