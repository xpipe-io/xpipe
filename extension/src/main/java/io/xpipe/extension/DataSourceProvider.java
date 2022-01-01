package io.xpipe.extension;

import io.xpipe.core.source.DataSourceDescriptor;
import io.xpipe.core.store.DataStore;

import java.nio.file.Path;

public interface DataSourceProvider {

    String getId();

    boolean supportsFile(Path file);

    DataSourceDescriptor<?> createDefaultDataSource(DataStore input) throws Exception;

    Class<? extends DataSourceDescriptor<?>> getType();
}
