package io.xpipe.core.source;


import io.xpipe.core.data.DataStructureNodeAcceptor;
import io.xpipe.core.data.generic.ArrayNode;
import io.xpipe.core.data.generic.TupleNode;
import io.xpipe.core.data.type.TupleType;

import java.io.OutputStream;

public interface DataTableConnection extends DataSourceConnection {

    TupleType determineDataType() throws Exception;

    int determineRowCount() throws Exception;

    void withLines(DataStructureNodeAcceptor<TupleNode> lineAcceptor) throws Exception;

    ArrayNode readLines(int maxLines) throws Exception;

    void forwardLines(OutputStream out, int maxLines) throws Exception;
}
