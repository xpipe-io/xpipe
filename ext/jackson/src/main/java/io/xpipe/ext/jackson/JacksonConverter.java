package io.xpipe.ext.jackson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import io.xpipe.core.data.node.ArrayNode;
import io.xpipe.core.data.node.DataStructureNode;
import io.xpipe.core.data.node.TupleNode;
import io.xpipe.core.data.node.ValueNode;

import java.math.BigInteger;
import java.util.ArrayList;

public class JacksonConverter {

    public static JsonNode convertToJson(DataStructureNode n) {
        if (n.isArray()) {
            var arrayNode = JsonNodeFactory.instance.arrayNode(n.asArray().size());
            for (var node : n.asArray().getNodes()) {
                arrayNode.add(convertToJson(node));
            }
            return arrayNode;
        }

        if (n.isTuple()) {
            var noKeys = n.getKeyNames().stream().allMatch(s -> s == null);
            if (noKeys) {
                return convertToJson(ArrayNode.of(n.getNodes()));
            }

            var objectNode = JsonNodeFactory.instance.objectNode();
            var index = 0;
            for (var entry : n.getKeyValuePairs()) {
                var key = entry.key() != null ? entry.key() : String.valueOf(index);
                index++;

                objectNode.set(key, convertToJson(entry.value()));
            }
            return objectNode;
        }

        if (n.isValue()) {
            if (n.hasMetaAttribute(DataStructureNode.IS_TEXT)) {
                return new TextNode(n.asString());
            } else if (n.hasMetaAttribute(DataStructureNode.IS_NULL)) {
                return NullNode.getInstance();
            } else if (n.hasMetaAttribute(DataStructureNode.BOOLEAN_TRUE)) {
                return BooleanNode.TRUE;
            } else if (n.hasMetaAttribute(DataStructureNode.BOOLEAN_FALSE)) {
                return BooleanNode.FALSE;
            } else if (n.hasMetaAttribute(DataStructureNode.IS_INTEGER)) {
                return BigIntegerNode.valueOf(new BigInteger(n.asString(), 10));
            } else if (n.hasMetaAttribute(DataStructureNode.IS_DECIMAL)) {
                return DoubleNode.valueOf(Double.parseDouble(n.asString()));
            } else {
                return new TextNode(n.asString());
            }
        }

        throw new AssertionError();
    }

    public static DataStructureNode convertFromJson(JsonNode n) {
        if (!n.isArray() && !n.isObject()) {
            if (n.isNull()) {
                return ValueNode.nullValue();
            }

            var value = ValueNode.of(n.isTextual() ? n.textValue() : n.toString());
            if (n.isTextual()) {
                value.tag(DataStructureNode.IS_TEXT);
            } else if (n.isBigDecimal() || n.isBigInteger() || n.isInt() || n.isLong()) {
                value.tag(DataStructureNode.IS_INTEGER);
            } else if (n.isBoolean()) {
                value.tag(DataStructureNode.IS_BOOLEAN);
                if (n.booleanValue()) {
                    value.tag(DataStructureNode.BOOLEAN_TRUE);
                } else {
                    value.tag(DataStructureNode.BOOLEAN_FALSE);
                }
            } else if (n.isFloatingPointNumber()) {
                value.tag(DataStructureNode.IS_DECIMAL);
            } else {
                throw new IllegalStateException();
            }
            return value;
        }

        if (n.isArray()) {
            var content = new ArrayList<DataStructureNode>();
            for (int i = 0; i < n.size(); i++) {
                content.add(convertFromJson(n.get(i)));
            }
            return ArrayNode.of(content);
        } else if (n.isObject()) {
            var b = TupleNode.builder();
            for (var it = n.fields(); it.hasNext(); ) {
                var kv = it.next();
                b.add(kv.getKey(), convertFromJson(kv.getValue()));
            }
            return b.build();
        } else {
            throw new AssertionError();
        }
    }
}
