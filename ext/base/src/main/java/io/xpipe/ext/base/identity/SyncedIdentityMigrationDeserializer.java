package io.xpipe.ext.base.identity;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.std.DelegatingDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TreeTraversingParser;

import java.io.IOException;

public class SyncedIdentityMigrationDeserializer extends DelegatingDeserializer {

    public SyncedIdentityMigrationDeserializer(JsonDeserializer<?> d) {
        super(d);
    }

    @Override
    protected JsonDeserializer<?> newDelegatingInstance(JsonDeserializer<?> newDelegatee) {
        return new SyncedIdentityMigrationDeserializer(newDelegatee);
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

        if (!node.isObject()) {
            var newJsonParser = new TreeTraversingParser((ObjectNode) node, p.getCodec());
            newJsonParser.nextToken();
            return newJsonParser;
        }

        migrate((ObjectNode) node);
        var newJsonParser = new TreeTraversingParser((ObjectNode) node, p.getCodec());
        newJsonParser.nextToken();
        return newJsonParser;
    }

    private void migrate(ObjectNode containerNode) {
        if (containerNode.has("perUser")) {
            containerNode.remove("perUser");
            containerNode.put("scope", "user");
        }
    }
}
