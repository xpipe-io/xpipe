package io.xpipe.core.source;

public interface RawWriteConnection extends DataSourceConnection {

    void write(byte[] bytes) throws Exception;
}
