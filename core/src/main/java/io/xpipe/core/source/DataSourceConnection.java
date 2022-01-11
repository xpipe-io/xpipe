package io.xpipe.core.source;

/**
 * Represents any type of connection to a data source.
 */
public interface DataSourceConnection extends AutoCloseable {

    /**
     * Initializes this connection. Required to be called
     * exactly once prior to attempting to use this connection.
     */
    void init() throws Exception;
}
