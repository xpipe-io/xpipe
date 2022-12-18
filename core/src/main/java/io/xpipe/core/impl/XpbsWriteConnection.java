package io.xpipe.core.impl;

import io.xpipe.core.data.generic.GenericDataStreamWriter;
import io.xpipe.core.data.node.DataStructureNode;
import io.xpipe.core.source.StructureWriteConnection;

public class XpbsWriteConnection extends StreamWriteConnection implements StructureWriteConnection {

    public XpbsWriteConnection(XpbsSource source) {
        super(source.getStore(), null);
    }

    @Override
    public void write(DataStructureNode node) throws Exception {
        GenericDataStreamWriter.writeStructure(outputStream, node);
    }
}
