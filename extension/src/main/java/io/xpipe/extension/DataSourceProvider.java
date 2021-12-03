package io.xpipe.extension;

import io.xpipe.core.source.DataSource;

public interface DataSourceProvider {

    String getId();

    Class<? extends DataSource<?>> getType();
}
