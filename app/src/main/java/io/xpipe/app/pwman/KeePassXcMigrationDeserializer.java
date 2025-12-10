package io.xpipe.app.pwman;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.DelegatingDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TreeTraversingParser;

import java.io.IOException;

public class KeePassXcMigrationDeserializer extends DelegatingDeserializer {

    public KeePassXcMigrationDeserializer(JsonDeserializer<?> d) {
        super(d);
    }

    @Override
    protected JsonDeserializer<?> newDelegatingInstance(JsonDeserializer<?> newDelegatee) {
        return new KeePassXcMigrationDeserializer(newDelegatee);
    }

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        return super.deserialize(restructure(p), ctxt);
    }

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt, Object intoValue) throws IOException {
        return super.deserialize(restructure(p), ctxt, intoValue);
    }

    public Object deserializeWithType(JsonParser jp, DeserializationContext ctxt, TypeDeserializer typeDeserializer)
            throws IOException {
        return super.deserializeWithType(restructure(jp), ctxt, typeDeserializer);
    }

    public JsonParser restructure(JsonParser p) throws IOException {
        var node = p.readValueAsTree();
        if (node == null) {
            return p;
        }

        if (node.isObject()) {
            var newJsonParser = new TreeTraversingParser(migrate((ObjectNode) node), p.getCodec());
            newJsonParser.nextToken();
            return newJsonParser;
        }

        var newJsonParser = new TreeTraversingParser((JsonNode) node, p.getCodec());
        newJsonParser.nextToken();
        return newJsonParser;
    }

    private ArrayNode migrate(ObjectNode containerNode) {
        var array = JsonNodeFactory.instance.arrayNode();
        array.add(containerNode);
        return array;
    }
}
