package io.xpipe.core.source;

import io.xpipe.core.data.node.DataStructureNode;
import io.xpipe.core.store.DataStore;
import lombok.experimental.SuperBuilder;

@SuperBuilder
public abstract class StructureDataSource<DS extends DataStore> extends DataSource<DS> {

    @Override
    public DataSourceType getType() {
        return DataSourceType.STRUCTURE;
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

    public final StructureReadConnection openReadConnection() {
        if (!isComplete()) {
            throw new UnsupportedOperationException();
        }

        return newReadConnection();
    }

    public final StructureWriteConnection openWriteConnection(WriteMode mode) {
        var con = newWriteConnection(mode);
        if (con == null) {
            throw new UnsupportedOperationException(mode.getId());
        }

        return con;
    }

    protected abstract StructureWriteConnection newWriteConnection(WriteMode mode);

    protected abstract StructureReadConnection newReadConnection();
}
