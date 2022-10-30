package io.xpipe.core.source;

import io.xpipe.core.data.node.DataStructureNode;
import io.xpipe.core.store.DataStore;
import lombok.experimental.SuperBuilder;

@SuperBuilder
public abstract class StructureDataSource<DS extends DataStore> extends DataSource<DS> {

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

    public final StructureWriteConnection openWriteConnection(WriteMode mode) throws Exception {
        var con = newWriteConnection(mode);
        if (con == null) {
            throw new UnsupportedOperationException(mode.getId());
        }

        con.init();
        return con;
    }

    protected abstract StructureWriteConnection newWriteConnection(WriteMode mode);

    protected abstract StructureReadConnection newReadConnection();
}
