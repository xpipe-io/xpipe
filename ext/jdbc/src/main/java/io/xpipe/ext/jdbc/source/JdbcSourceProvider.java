package io.xpipe.ext.jdbc.source;

import io.xpipe.core.source.DataSourceType;
import io.xpipe.core.store.DataStore;
import io.xpipe.ext.jdbc.JdbcBaseStore;
import io.xpipe.extension.DataSourceProvider;

public abstract class JdbcSourceProvider<T extends JdbcSource> implements DataSourceProvider<T> {

    @Override
    public Category getCategory() {
        return Category.DATABASE;
    }

    @Override
    public DataSourceType getPrimaryType() {
        return DataSourceType.TABLE;
    }

    @Override
    public boolean prefersStore(DataStore store, DataSourceType type) {
        return store instanceof JdbcBaseStore;
    }

    @Override
    public boolean couldSupportStore(DataStore store) {
        return store instanceof JdbcBaseStore;
    }

    @Override
    public abstract Class<T> getSourceClass();

    @Override
    public String getDisplayIconFileName() {
        return "jdbc:" + getId() + "_icon.png";
    }
}
