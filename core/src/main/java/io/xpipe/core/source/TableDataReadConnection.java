package io.xpipe.core.source;


import io.xpipe.core.data.node.DataStructureNodeAcceptor;
import io.xpipe.core.data.node.ArrayNode;
import io.xpipe.core.data.node.TupleNode;
import io.xpipe.core.data.type.TupleType;

import java.io.OutputStream;

public interface TableDataReadConnection extends DataSourceConnection {

    TupleType getDataType() throws Exception;

    int getRowCount() throws Exception;

    void withLines(DataStructureNodeAcceptor<TupleNode> lineAcceptor) throws Exception;

    ArrayNode readLines(int maxLines) throws Exception;

    void forwardLines(OutputStream out, int maxLines) throws Exception;
}
