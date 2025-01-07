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

public class IdentityMigrationDeserializer extends DelegatingDeserializer {

    public IdentityMigrationDeserializer(JsonDeserializer<?> d) {
        super(d);
    }

    @Override
    protected JsonDeserializer<?> newDelegatingInstance(JsonDeserializer<?> newDelegatee) {
        return new IdentityMigrationDeserializer(newDelegatee);
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
        if (!node.isObject()) {
            return p;
        }

        migrate((ObjectNode) node);
        var newJsonParser = new TreeTraversingParser((ObjectNode) node, p.getCodec());
        newJsonParser.nextToken();
        return newJsonParser;
    }

    private void migrate(ObjectNode containerNode) {
        var user = containerNode.get("user");
        var password = containerNode.get("password");
        var identity = containerNode.get("identityStrategy");
        if (identity == null) {
            var vmIdentity = containerNode.get("identity");
            if (vmIdentity != null && !vmIdentity.has("username")) {
                identity = vmIdentity;
            }
        }
        if (identity == null) {
            var additional = containerNode.get("additionalIdentity");
            if (additional != null) {
                identity = additional;
            }
        }

        if (password != null && password.isObject() && identity != null && identity.isObject()) {
            var identityStore = JsonNodeFactory.instance.objectNode();
            identityStore.put("type", "localIdentity");
            if (user != null && user.isTextual()) {
                identityStore.set("username", user);
            }
            identityStore.set("password", password);
            identityStore.set("sshIdentity", identity);

            var inPlace = JsonNodeFactory.instance.objectNode();
            inPlace.put("type", "inPlace");
            inPlace.set("identityStore", identityStore);

            containerNode.set("identity", inPlace);
        }
    }
}
