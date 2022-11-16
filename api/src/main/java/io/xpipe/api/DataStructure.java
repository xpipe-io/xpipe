package io.xpipe.api;

import io.xpipe.core.data.node.DataStructureNode;

public interface DataStructure extends DataSource {
    DataStructureNode read();
}
