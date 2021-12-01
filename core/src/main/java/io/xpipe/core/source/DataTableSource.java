package io.xpipe.core.source;

import io.xpipe.core.store.DataStore;

public abstract class DataTableSource<DS extends DataStore> implements DataSource<DS> {

    public abstract DataTableWriteConnection openWriteConnection(DS store);

    public abstract DataTableConnection openConnection(DS store);

    @Override
    public DataSourceType getType() {
        return DataSourceType.TABLE;
    }
}
