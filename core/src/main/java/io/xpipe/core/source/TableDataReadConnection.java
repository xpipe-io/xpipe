package io.xpipe.core.source;


import io.xpipe.core.data.DataStructureNodeAcceptor;
import io.xpipe.core.data.node.ArrayNode;
import io.xpipe.core.data.node.SimpleTupleNode;
import io.xpipe.core.data.type.TupleType;

import java.io.OutputStream;

public interface TableDataReadConnection extends DataSourceConnection {

    TupleType determineDataType() throws Exception;

    int determineRowCount() throws Exception;

    void withLines(DataStructureNodeAcceptor<SimpleTupleNode> lineAcceptor) throws Exception;

    ArrayNode readLines(int maxLines) throws Exception;

    void forwardLines(OutputStream out, int maxLines) throws Exception;
}
