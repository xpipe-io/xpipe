package io.xpipe.core.source;

public interface DataSourceReadConnection extends DataSourceConnection {

    boolean canRead() throws Exception;

    void forward(DataSourceConnection con) throws Exception;
}
