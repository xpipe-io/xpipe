package io.xpipe.core.source;

public interface DataSourceConnection extends AutoCloseable {

    void init() throws Exception;

    void close() throws Exception;
}
