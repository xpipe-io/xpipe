package io.xpipe.core.impl;

import io.xpipe.core.source.DataSource;
import io.xpipe.core.source.DataSourceConnection;
import lombok.Getter;

public class SimpleDataSourceConnection<T extends DataSource<?>> implements DataSourceConnection {

    @Getter
    protected final T source;

    public SimpleDataSourceConnection(T source) {
        this.source = source;
    }
}
