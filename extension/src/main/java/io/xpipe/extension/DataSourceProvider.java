package io.xpipe.extension;

import io.xpipe.core.source.DataSourceDescriptor;

public interface DataSourceProvider {

    String getId();

    Class<? extends DataSourceDescriptor<?>> getType();
}
