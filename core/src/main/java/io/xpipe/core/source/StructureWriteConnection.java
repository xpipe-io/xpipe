package io.xpipe.core.source;

import io.xpipe.core.data.node.DataStructureNode;

public interface StructureWriteConnection extends DataSourceConnection {

    /**
     * Writes the contents to the data source.
     */
    void write(DataStructureNode node) throws Exception;
}
