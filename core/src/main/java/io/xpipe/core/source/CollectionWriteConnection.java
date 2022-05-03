package io.xpipe.core.source;

public interface CollectionWriteConnection extends DataSourceConnection {

    void write(String entry, DataSourceReadConnection con) throws Exception;
}
