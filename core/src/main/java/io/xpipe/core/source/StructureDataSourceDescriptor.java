package io.xpipe.core.source;

import io.xpipe.core.store.DataStore;

public abstract class StructureDataSourceDescriptor<DS extends DataStore> implements DataSourceDescriptor<DS> {

    public final StructureReadConnection openReadConnection(DS store) throws Exception {
        var con = newReadConnection(store);
        con.init();
        return con;
    }

    public final StructureWriteConnection openWriteConnection(DS store) throws Exception {
        var con = newWriteConnection(store);
        con.init();
        return con;
    }

    protected abstract StructureWriteConnection newWriteConnection(DS store);

    protected abstract StructureReadConnection newReadConnection(DS store);

    @Override
    public DataSourceType getType() {
        return DataSourceType.STRUCTURE;
    }
}
