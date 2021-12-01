package io.xpipe.core.data.generic;

import io.xpipe.core.data.DataStructureNode;

public interface DataStructureNodeReader extends DataStreamCallback {

    boolean isDone();

    DataStructureNode create();
}
