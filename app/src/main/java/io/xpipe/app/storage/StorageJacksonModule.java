package io.xpipe.app.storage;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.xpipe.app.util.PasswordLockSecretValue;
import io.xpipe.app.util.VaultKeySecretValue;
import io.xpipe.core.store.LocalStore;
import io.xpipe.core.util.EncryptedSecretValue;
import io.xpipe.core.util.JacksonMapper;
import io.xpipe.core.util.SecretValue;

import java.io.IOException;
import java.util.UUID;

public class StorageJacksonModule extends SimpleModule {

    @Override
    public void setupModule(SetupContext context) {
        context.registerSubtypes(VaultKeySecretValue.class);
        context.registerSubtypes(PasswordLockSecretValue.class);

        addSerializer(DataStoreEntryRef.class, new DataStoreEntryRefSerializer());
        addDeserializer(DataStoreEntryRef.class, new DataStoreEntryRefDeserializer());
        addSerializer(ContextualFileReference.class, new LocalFileReferenceSerializer());
        addDeserializer(ContextualFileReference.class, new LocalFileReferenceDeserializer());
        addSerializer(DataStoreSecret.class, new DataStoreSecretSerializer());
        addDeserializer(DataStoreSecret.class, new DataStoreSecretDeserializer());

        context.addSerializers(_serializers);
        context.addDeserializers(_deserializers);
    }

    public static class LocalFileReferenceSerializer extends JsonSerializer<ContextualFileReference> {

        @Override
        public void serialize(ContextualFileReference value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            jgen.writeString(value.serialize());
        }
    }

    public static class LocalFileReferenceDeserializer extends JsonDeserializer<ContextualFileReference> {

        @Override
        public ContextualFileReference deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            return ContextualFileReference.of(p.getValueAsString());
        }
    }

    public static class DataStoreSecretSerializer extends JsonSerializer<DataStoreSecret> {

        @Override
        public void serialize(DataStoreSecret value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            // Preserve same output if not changed
            if (value.getOriginalNode() != null && !value.requiresRewrite()) {
                var tree = JsonNodeFactory.instance.objectNode();
                tree.set("secret", (JsonNode) value.getOriginalNode());
                jgen.writeTree(tree);
                return;
            }

            // Reencrypt
            var val = value.getOutputSecret();
            var valTree = JacksonMapper.getDefault().valueToTree(val);
            var tree = JsonNodeFactory.instance.objectNode();
            tree.set("secret", valTree);
            jgen.writeTree(tree);
            value.setOriginalNode(valTree);
        }
    }

    public static class DataStoreSecretDeserializer extends JsonDeserializer<DataStoreSecret> {

        @Override
        public DataStoreSecret deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            var tree = JacksonMapper.getDefault().readTree(p);
            if (!tree.isObject()) {
                return null;
            }

            var legacy = JacksonMapper.getDefault().treeToValue(tree, EncryptedSecretValue.class);
            if (legacy != null) {
                // Don't cache legacy node
                return new DataStoreSecret(null, legacy.inPlace());
            }

            var obj = (ObjectNode) tree;
            if (!obj.has("secret")) {
                return null;
            }

            var secretTree = obj.required("secret");
            var secret = JacksonMapper.getDefault().treeToValue(secretTree, SecretValue.class);
            if (secret == null) {
                return null;
            }

            return new DataStoreSecret(secretTree, secret.inPlace());
        }
    }

    @SuppressWarnings("all")
    public static class DataStoreEntryRefSerializer extends JsonSerializer<DataStoreEntryRef> {

        @Override
        public void serialize(DataStoreEntryRef value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            if (value == null) {
                jgen.writeNull();
                return;
            }

            jgen.writeStartObject();
            jgen.writeFieldName("storeId");
            jgen.writeString(value.getEntry().getUuid().toString());
            jgen.writeEndObject();
        }
    }

    public static class DataStoreEntryRefDeserializer extends JsonDeserializer<DataStoreEntryRef<?>> {

        @Override
        public DataStoreEntryRef<?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            var obj = (ObjectNode) p.getCodec().readTree(p);
            if (!obj.has("storeId") || !obj.required("storeId").isTextual()) {
                return null;
            }

            var text = obj.required("storeId").asText();
            if (text.isBlank()) {
                return null;
            }

            var id = UUID.fromString(text);
            var e = DataStorage.get().getStoreEntryIfPresent(id).filter(dataStoreEntry -> dataStoreEntry.getValidity() != DataStoreEntry.Validity.LOAD_FAILED).orElse(null);
            if (e == null) {
                return null;
            }

            // Compatibility fix for legacy local stores
            var toUse = e.getStore() instanceof LocalStore ? DataStorage.get().local() : e;
            return toUse != null ? new DataStoreEntryRef<>(toUse) : null;
        }
    }
}
