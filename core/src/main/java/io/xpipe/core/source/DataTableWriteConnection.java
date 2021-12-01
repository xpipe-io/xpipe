package io.xpipe.core.source;

import io.xpipe.core.data.DataStructureNodeAcceptor;
import io.xpipe.core.data.generic.ArrayNode;
import io.xpipe.core.data.generic.TupleNode;

public interface DataTableWriteConnection extends DataSourceConnection {

    DataStructureNodeAcceptor<TupleNode> writeLinesAcceptor();

    void writeLines(ArrayNode lines) throws Exception;
}
