package io.xpipe.ext.jackson.xml;

import com.fasterxml.jackson.dataformat.xml.XmlFactory;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import com.fasterxml.jackson.dataformat.xml.util.DefaultXmlPrettyPrinter;
import io.xpipe.core.data.node.DataStructureNode;
import io.xpipe.core.impl.StreamWriteConnection;
import io.xpipe.core.source.StructureWriteConnection;
import io.xpipe.core.util.JacksonMapper;
import io.xpipe.ext.jackson.JacksonConverter;

import javax.xml.namespace.QName;
import java.io.OutputStreamWriter;

public class XmlWriteConnection extends StreamWriteConnection implements StructureWriteConnection {
    private final XmlProvider.Source source;

    public XmlWriteConnection(XmlProvider.Source source) {
        super(source.getStore(), source.getCharset());
        this.source = source;
    }

    @Override
    public void write(DataStructureNode node) throws Exception {
        var convertedNode = JacksonConverter.convertToJson(node);
        var writer = new OutputStreamWriter(outputStream);
        XmlFactory f = new XmlFactory();
        f.configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true);
        try (ToXmlGenerator g =
                (ToXmlGenerator) f.createGenerator(writer).setPrettyPrinter(new DefaultXmlPrettyPrinter())) {
            g.setNextName(new QName("root"));
            JacksonMapper.newMapper().writeTree(g, convertedNode);
        }
    }
}
