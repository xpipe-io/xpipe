package io.xpipe.ext.base.script;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.std.DelegatingDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TreeTraversingParser;

import java.io.IOException;

public class ScriptStoreMigrationDeserializer extends DelegatingDeserializer {

    public ScriptStoreMigrationDeserializer(JsonDeserializer<?> d) {
        super(d);
    }

    @Override
    protected JsonDeserializer<?> newDelegatingInstance(JsonDeserializer<?> newDelegatee) {
        return new ScriptStoreMigrationDeserializer(newDelegatee);
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
        if (node == null || !node.isObject()) {
            return p;
        }

        // Check if already in the new format
        if (node.get("textSource") == null) {
            migrate((ObjectNode) node);
        }

        var newJsonParser = new TreeTraversingParser(((ObjectNode) node), p.getCodec());
        newJsonParser.nextToken();
        return newJsonParser;
    }

    private void migrate(ObjectNode n) {
        var commandsNode = n.remove("commands");
        var dialectNode = n.remove("minimumDialect");

        var obj = JsonNodeFactory.instance.objectNode();
        obj.put("type", "inPlace");
        obj.put("text", commandsNode.textValue());
        if (!dialectNode.isNull()) {
            obj.put("dialect", dialectNode.textValue());
        } else {
            obj.putNull("dialect");
        }

        n.set("textSource", obj);
    }
}
