package io.xpipe.core.source;

import io.xpipe.core.store.DataStore;

public interface CollectionDataSourceDescriptor<DS extends DataStore> extends DataSourceDescriptor<DS> {

    @Override
    default DataSourceInfo determineInfo(DS store) throws Exception {
        try (var con = openReadConnection(store)) {
            var c = (int) con.listEntries().count();
            return new DataSourceInfo.Structure(c);
        }
    }

    default CollectionReadConnection openReadConnection(DS store) throws Exception {
        var con = newReadConnection(store);
        con.init();
        return con;
    }

    default CollectionWriteConnection openWriteConnection(DS store) throws Exception {
        var con = newWriteConnection(store);
        con.init();
        return con;
    }

    CollectionWriteConnection newWriteConnection(DS store);

    CollectionReadConnection newReadConnection(DS store);
}
