package io.xpipe.core.source;

import io.xpipe.core.data.node.DataStructureNode;
import io.xpipe.core.store.DataStore;

public interface StructureDataSourceDescriptor<DS extends DataStore> extends DataSourceDescriptor<DS> {

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
    default DataSourceInfo determineInfo(DS store) throws Exception {
        try (var con = openReadConnection(store)) {
            var n = con.read();
            var c = countEntries(n);
            return new DataSourceInfo.Structure(c);
        }
    }

    default StructureReadConnection openReadConnection(DS store) throws Exception {
        var con = newReadConnection(store);
        con.init();
        return con;
    }

    default StructureWriteConnection openWriteConnection(DS store) throws Exception {
        var con = newWriteConnection(store);
        con.init();
        return con;
    }
    StructureWriteConnection newWriteConnection(DS store);

    StructureReadConnection newReadConnection(DS store);
}
