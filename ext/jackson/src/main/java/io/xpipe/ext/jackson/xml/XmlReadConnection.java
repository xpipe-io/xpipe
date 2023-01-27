package io.xpipe.ext.jackson.xml;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.xpipe.core.data.node.DataStructureNode;
import io.xpipe.core.impl.StreamReadConnection;
import io.xpipe.core.source.StructureReadConnection;
import io.xpipe.ext.jackson.JacksonConverter;

public class XmlReadConnection extends StreamReadConnection implements StructureReadConnection {

    public XmlReadConnection(XmlProvider.Source source) {
        super(source.getStore(), source.getCharset());
    }

    @Override
    public DataStructureNode read() throws Exception {
        XmlMapper o = new XmlMapper();
        var node = o.readTree(inputStream.readAllBytes());
        return JacksonConverter.convertFromJson(node);
    }
}
