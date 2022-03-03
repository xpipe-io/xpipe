package io.xpipe.api;

import io.xpipe.core.data.node.DataStructureNode;
import io.xpipe.core.source.DataSourceInfo;

public interface DataStructure extends DataSource {

    DataSourceInfo.Structure getInfo();

    DataStructureNode read();
}
