package io.xpipe.ext.jackson.xml_table;

import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.xml.XmlFactory;
import com.fasterxml.jackson.dataformat.xml.deser.FromXmlParser;
import io.xpipe.core.charsetter.Charsetter;
import io.xpipe.core.charsetter.StreamCharset;
import io.xpipe.core.data.node.DataStructureNodeAcceptor;
import io.xpipe.core.data.node.TupleNode;
import io.xpipe.core.data.type.ArrayType;
import io.xpipe.core.data.type.TupleType;
import io.xpipe.core.data.type.ValueType;
import io.xpipe.core.data.type.WildcardType;
import io.xpipe.core.impl.PeekTableStreamReadConnection;
import io.xpipe.core.store.StreamDataStore;
import io.xpipe.ext.jackson.JacksonConverter;

import java.util.Collections;
import java.util.List;

public class XmlTableReadConnection extends PeekTableStreamReadConnection {

    private TupleType type;

    public XmlTableReadConnection(XmlTableProvider.Source source) {
        super(source.getStore(), source.getCharset());
    }

    public static DetectedTable detect(StreamDataStore store, StreamCharset charset) throws Exception {
        if (!store.canOpen()) {
            return new DetectedTable(null, null);
        }

        var reader = Charsetter.get().reader(store, charset);

        // Check for an completely empty file
        reader.mark(1);
        var read = reader.read();
        reader.reset();
        if (read == -1) {
            return new DetectedTable(null, null);
        }

        XmlFactory jfactory = new XmlFactory();
        FromXmlParser parser = (FromXmlParser) jfactory.createParser(reader);
        if (parser.nextToken() != JsonToken.START_OBJECT) {
            throw new IllegalStateException("Unexpected token: " + parser.getCurrentToken());
        }
        parser.nextFieldName();
        return new DetectedTable(null, null);
    }

    @Override
    public TupleType getDataType() {
        return type;
    }

    @Override
    protected void withRowsInternal(DataStructureNodeAcceptor<TupleNode> lineAcceptor) throws Exception {
        XmlFactory jfactory = new XmlFactory();
        FromXmlParser parser = (FromXmlParser) jfactory.createParser(inputStream);
        if (parser.nextToken() != JsonToken.START_OBJECT) {
            throw new IllegalStateException("Unexpected token: " + parser.getCurrentToken());
        }
        parser.nextToken();
        var mapper = new JsonMapper();

        while (parser.nextToken() != JsonToken.END_OBJECT) {
            parser.nextToken();
            var tree = mapper.readTree(parser);
            var converted = update(tree);
            if (!lineAcceptor.accept(converted)) {
                break;
            }
        }
    }

    private TupleNode update(TreeNode tree) {
        var converted = JacksonConverter.convertFromJson((JsonNode) tree);

        if (type != null && !type.matches(converted)) {
            throw new IllegalStateException("Inconsistent array type");
        }

        if (type == null) {
            if (converted.isValue()) {
                type = TupleType.of(Collections.singletonList(null), Collections.singletonList(ValueType.of()));
            } else if (converted.isArray()) {
                type = TupleType.of(
                        Collections.singletonList(null), Collections.singletonList(ArrayType.of(WildcardType.of())));
            } else {
                type = converted.determineDataType().asTuple();
            }
        }

        if (converted.isValue()) {
            return TupleNode.of(List.of(converted));
        } else if (converted.isArray()) {
            return TupleNode.of(List.of(converted));
        } else {
            return converted.asTuple();
        }
    }

    public record DetectedTable(String rootName, String entryName) {}
}
