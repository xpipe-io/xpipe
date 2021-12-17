package io.xpipe.core.data.generic;

import io.xpipe.core.data.DataStructureNode;

public interface GenericAbstractReader extends GenericDataStreamCallback {

    boolean isDone();

    DataStructureNode create();
}
