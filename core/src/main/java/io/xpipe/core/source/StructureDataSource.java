package io.xpipe.core.source;

import io.xpipe.core.data.node.DataStructureNode;
import io.xpipe.core.store.DataStore;

public abstract class StructureDataSource<DS extends DataStore> extends DataSource<DS> {

    public StructureDataSource(DS store) {
        super(store);
    }

    private int countEntries(DataStructureNode n) {
        if (n.isValue()) {
            return 1;
        }

        int c = 0;
        for (int i = 0; i < n.size(); i++) {
            c += countEntries(n.at(i));
        }
        return c;
    }

    @Override
    public final DataSourceInfo determineInfo() throws Exception {
        try (var con = openReadConnection()) {
            var n = con.read();
            var c = countEntries(n);
            return new DataSourceInfo.Structure(c);
        }
    }

    public final StructureReadConnection openReadConnection() throws Exception {
        var con = newReadConnection();
        con.init();
        return con;
    }

    public final StructureWriteConnection openWriteConnection() throws Exception {
        var con = newWriteConnection();
        con.init();
        return con;
    }

    protected abstract StructureWriteConnection newWriteConnection();

    protected abstract StructureReadConnection newReadConnection();
}
