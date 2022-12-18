package io.xpipe.core.impl;

import io.xpipe.core.data.generic.GenericDataStreamParser;
import io.xpipe.core.data.node.DataStructureNode;
import io.xpipe.core.source.StructureReadConnection;

public class XpbsReadConnection extends StreamReadConnection implements StructureReadConnection {

    public XpbsReadConnection(XpbsSource source) {
        super(source.getStore(), null);
    }

    @Override
    public DataStructureNode read() throws Exception {
        return GenericDataStreamParser.parse(inputStream);
    }
}
