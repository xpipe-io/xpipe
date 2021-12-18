package io.xpipe.core.data.typed;

import io.xpipe.core.data.node.DataStructureNode;

public interface TypedAbstractReader extends TypedDataStreamCallback {

    boolean isDone();

    DataStructureNode create();
}
