package io.xpipe.core.source;

import io.xpipe.core.data.node.DataStructureNode;

public interface StructureReadConnection extends DataSourceConnection {

    /**
     * Reads the complete contents.
     */
    DataStructureNode read() throws Exception;
}
