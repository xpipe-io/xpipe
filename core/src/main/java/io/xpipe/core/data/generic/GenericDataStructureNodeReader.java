package io.xpipe.core.data.generic;

import io.xpipe.core.data.DataStructureNode;

public interface GenericDataStructureNodeReader extends GenericDataStreamCallback {

    boolean isDone();

    DataStructureNode create();
}
