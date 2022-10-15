package io.xpipe.core.source;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum WriteMode {
    @JsonProperty("replace")
    REPLACE(DataSource::openWriteConnection),
    @JsonProperty("append")
    APPEND(DataSource::openAppendingWriteConnection),
    @JsonProperty("prepend")
    PREPEND(DataSource::openPrependingWriteConnection);

    private final FailableFunction<DataSource<?>, DataSourceConnection, Exception> connectionOpener;

    WriteMode(FailableFunction<DataSource<?>, DataSourceConnection, Exception> connectionOpener) {
        this.connectionOpener = connectionOpener;
    }

    public DataSourceConnection open(DataSource<?> source) throws Exception {
        return connectionOpener.apply(source);
    }

    public static interface FailableFunction<T, R, E extends Throwable> {
        R apply(T input) throws E;
    }
}
