package io.xpipe.core.source;

public interface DataSourceReadConnection extends DataSourceConnection {

    void forward(DataSourceConnection con) throws Exception;
}
