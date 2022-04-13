package io.xpipe.core.source;

import io.xpipe.core.data.node.DataStructureNode;

public interface StructureReadConnection extends DataSourceReadConnection {

    /**
     * Reads the complete contents.
     */
    DataStructureNode read() throws Exception;

    default void forward(DataSourceConnection con) throws Exception {
        try (var tCon = (StructureWriteConnection) con) {
            tCon.init();
            tCon.write(read());
        }
    }
}
