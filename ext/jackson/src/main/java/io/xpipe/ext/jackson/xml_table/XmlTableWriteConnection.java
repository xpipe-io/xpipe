package io.xpipe.ext.jackson.xml_table;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlFactory;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import com.fasterxml.jackson.dataformat.xml.util.DefaultXmlPrettyPrinter;
import io.xpipe.core.data.node.DataStructureNode;
import io.xpipe.core.data.node.DataStructureNodeAcceptor;
import io.xpipe.core.data.node.TupleNode;
import io.xpipe.core.impl.SimpleTableWriteConnection;
import io.xpipe.core.impl.StreamWriteConnection;
import io.xpipe.core.source.TableMapping;
import io.xpipe.ext.jackson.JacksonConverter;
import lombok.Getter;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class XmlTableWriteConnection extends StreamWriteConnection
        implements SimpleTableWriteConnection<XmlTableProvider.Source> {

    @Getter
    private final XmlTableProvider.Source source;

    private ToXmlGenerator generator;
    private boolean hasWrittenRoot;

    public XmlTableWriteConnection(XmlTableProvider.Source source) {
        super(source.getStore(), source.getCharset());
        this.source = source;
    }

    @Override
    public void init() throws Exception {
        super.init();

        var writer = new OutputStreamWriter(outputStream);
        writer.append("<?xml version=\"1.0\"?>").append(source.getNewLine().getNewLineString());
        XmlFactory f = new XmlFactory();
        // TO DO: Fix new line characters
        generator = (ToXmlGenerator) f.createGenerator(writer).setPrettyPrinter(new DefaultXmlPrettyPrinter());
        generator.setCodec(new XmlMapper().enable(SerializationFeature.INDENT_OUTPUT));
    }

    private void writeRootNameIfNecessary(DataStructureNode n) throws IOException {
        if (hasWrittenRoot) {
            return;
        }

        var rootName = source.getRootName() != null
                ? source.getRootName()
                : n.getMetaAttribute(DataStructureNode.KEY_TABLE_NAME) != null
                        ? n.getMetaAttribute(DataStructureNode.KEY_TABLE_NAME)
                        : "Content";
        generator.setNextName(new QName(rootName));
        generator.writeStartObject();
        hasWrittenRoot = true;
    }

    private void writeEntryName(DataStructureNode n) throws IOException {

        var entryName = source.getEntryName() != null
                ? source.getEntryName()
                : n.getMetaAttribute(DataStructureNode.KEY_TABLE_NAME) != null
                        ? n.getMetaAttribute(DataStructureNode.KEY_TABLE_NAME)
                        : "Entry";
        generator.setNextName(new QName(entryName));
        generator.writeFieldName(entryName);
        // generator.writeStartObject();
    }

    @Override
    public void close() throws Exception {
        generator.writeEndObject();
        generator.close();
        super.close();
    }

    @Override
    public DataStructureNodeAcceptor<TupleNode> writeLinesAcceptor(TableMapping mapping) {
        return node -> {
            var converted = JacksonConverter.convertToJson(node);
            writeRootNameIfNecessary(node);
            writeEntryName(node);
            generator.writeTree(converted);
            // generator.writeEndObject();
            return true;
        };
    }
}
