package io.xpipe.ext.collections;

import io.xpipe.core.source.DataSource;
import io.xpipe.core.source.DataSourceType;
import io.xpipe.ext.base.SimpleFileDataSourceProvider;

public abstract class ArchiveFileProvider<T extends DataSource<?>> implements SimpleFileDataSourceProvider<T> {

    @Override
    public DataSourceType getPrimaryType() {
        return DataSourceType.COLLECTION;
    }

    @Override
    public String getDisplayIconFileName() {
        return "collections:" + getId() + "_icon.png";
    }
}
