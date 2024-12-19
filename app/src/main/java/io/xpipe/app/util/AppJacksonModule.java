package io.xpipe.app.util;

import io.xpipe.app.ext.LocalStore;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.*;
import io.xpipe.app.terminal.ExternalTerminalType;
import io.xpipe.core.util.EncryptedSecretValue;
import io.xpipe.core.util.JacksonMapper;
import io.xpipe.core.util.SecretValue;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.UUID;

public class AppJacksonModule extends SimpleModule {

    @Override
    public void setupModule(SetupContext context) {
        context.registerSubtypes(VaultKeySecretValue.class);
        context.registerSubtypes(PasswordLockSecretValue.class);

        addSerializer(DataStoreEntryRef.class, new DataStoreEntryRefSerializer());
        addDeserializer(DataStoreEntryRef.class, new DataStoreEntryRefDeserializer());
        addSerializer(ContextualFileReference.class, new LocalFileReferenceSerializer());
        addDeserializer(ContextualFileReference.class, new LocalFileReferenceDeserializer());
        addSerializer(DataStorageSecret.class, new DataStoreSecretSerializer());
        addDeserializer(DataStorageSecret.class, new DataStoreSecretDeserializer());
        addSerializer(ExternalTerminalType.class, new ExternalTerminalTypeSerializer());
        addDeserializer(ExternalTerminalType.class, new ExternalTerminalTypeDeserializer());

        context.addSerializers(_serializers);
        context.addDeserializers(_deserializers);
    }

    public static class LocalFileReferenceSerializer extends JsonSerializer<ContextualFileReference> {

        @Override
        public void serialize(ContextualFileReference value, JsonGenerator jgen, SerializerProvider provider)
                throws IOException {
            jgen.writeString(value.serialize());
        }
    }

    public static class LocalFileReferenceDeserializer extends JsonDeserializer<ContextualFileReference> {

        @Override
        public ContextualFileReference deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            return ContextualFileReference.of(p.getValueAsString());
        }
    }

    public static class ExternalTerminalTypeSerializer extends JsonSerializer<ExternalTerminalType> {

        @Override
        public void serialize(ExternalTerminalType value, JsonGenerator jgen, SerializerProvider provider)
                throws IOException {
            jgen.writeString(value.getId());
        }
    }

    public static class ExternalTerminalTypeDeserializer extends JsonDeserializer<ExternalTerminalType> {

        @Override
        public ExternalTerminalType deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            var id = p.getValueAsString();
            return ExternalTerminalType.ALL_ON_ALL_PLATFORMS.stream()
                    .filter(terminalType -> terminalType.getId().equals(id))
                    .findFirst()
                    .orElse(null);
        }
    }

    public static class DataStoreSecretSerializer extends JsonSerializer<DataStorageSecret> {

        @Override
        public void serialize(DataStorageSecret value, JsonGenerator jgen, SerializerProvider provider)
                throws IOException {
            var mapper = JacksonMapper.getDefault();
            var tree = JsonNodeFactory.instance.objectNode();
            tree.set("encryptedToken", mapper.valueToTree(value.getEncryptedToken()));

            // Preserve same output if not changed
            if (value.getOriginalNode() != null && !value.requiresRewrite()) {
                tree.set("secret", (JsonNode) value.getOriginalNode());
                jgen.writeTree(tree);
                return;
            }

            // Reencrypt
            var val = value.rewrite();
            tree.set("secret", val);
            jgen.writeTree(tree);
        }
    }

    public static class DataStoreSecretDeserializer extends JsonDeserializer<DataStorageSecret> {

        @Override
        public DataStorageSecret deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            var tree = JacksonMapper.getDefault().readTree(p);
            if (!tree.isObject()) {
                return null;
            }

            var legacy = JacksonMapper.getDefault().treeToValue(tree, EncryptedSecretValue.class);
            if (legacy != null) {
                // Don't cache legacy node
                return new DataStorageSecret(EncryptionToken.ofVaultKey(), null, legacy.inPlace());
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

            var hadLock = AppPrefs.get().getLockCrypt().get() != null
                    && !AppPrefs.get().getLockCrypt().get().isEmpty();
            var tokenNode = obj.get("encryptedToken");
            var token =
                    tokenNode != null ? JacksonMapper.getDefault().treeToValue(tokenNode, EncryptionToken.class) : null;
            if (token == null) {
                var migrateToUser = hadLock;
                if (migrateToUser && DataStorageUserHandler.getInstance().getActiveUser() == null) {
                    return null;
                }
                token = migrateToUser ? EncryptionToken.ofUser() : EncryptionToken.ofVaultKey();
            }

            return new DataStorageSecret(token, secretTree, secret);
        }
    }

    @SuppressWarnings("all")
    public static class DataStoreEntryRefSerializer extends JsonSerializer<DataStoreEntryRef> {

        @Override
        public void serialize(DataStoreEntryRef value, JsonGenerator jgen, SerializerProvider provider)
                throws IOException {
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
            // Keep an invalid entry if it is per-user, meaning that it will get removed later on
            var e = DataStorage.get()
                    .getStoreEntryIfPresent(id)
                    .filter(dataStoreEntry -> dataStoreEntry.getValidity() != DataStoreEntry.Validity.LOAD_FAILED
                            || !dataStoreEntry.getStoreNode().isAvailableForUser())
                    .orElse(null);
            if (e == null) {
                return null;
            }

            // Compatibility fix for legacy local stores
            var toUse = e.getStore() instanceof LocalStore ? DataStorage.get().local() : e;
            if (toUse == null) {
                return null;
            }

            return new DataStoreEntryRef<>(toUse);
        }
    }
}
