package io.xpipe.ext.jackson.json_table;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonNode;
import io.xpipe.core.data.node.DataStructureNodeAcceptor;
import io.xpipe.core.data.node.TupleNode;
import io.xpipe.core.data.type.ArrayType;
import io.xpipe.core.data.type.TupleType;
import io.xpipe.core.data.type.ValueType;
import io.xpipe.core.data.type.WildcardType;
import io.xpipe.core.impl.StreamReadConnection;
import io.xpipe.core.source.TableReadConnection;
import io.xpipe.core.util.JacksonMapper;
import io.xpipe.ext.jackson.JacksonConverter;

import java.util.Collections;
import java.util.List;

public class JsonTableReadConnection extends StreamReadConnection implements TableReadConnection {

    private TupleType type;

    public JsonTableReadConnection(JsonTableProvider.Source source) {
        super(source.getStore(), source.getCharset());
    }

    @Override
    public TupleType getDataType() {
        return type;
    }

    @Override
    public void withRows(DataStructureNodeAcceptor<TupleNode> lineAcceptor) throws Exception {
        JsonFactory jfactory = new JsonFactory();
        JsonParser parser = jfactory.createParser(inputStream);
        if (parser.nextToken() != JsonToken.START_ARRAY) {
            throw new IllegalStateException("Unexpected token: " + parser.getCurrentToken());
        }
        var mapper = JacksonMapper.newMapper();

        while (parser.nextToken() != JsonToken.END_ARRAY) {
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
}
