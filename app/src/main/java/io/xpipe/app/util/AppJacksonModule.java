package io.xpipe.app.util;

import io.xpipe.app.ext.LocalStore;
import io.xpipe.app.storage.*;
import io.xpipe.app.terminal.ExternalTerminalType;
import io.xpipe.core.util.InPlaceSecretValue;
import io.xpipe.core.util.JacksonMapper;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.jsontype.impl.AsPropertyTypeDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.type.SimpleType;

import java.io.CharArrayReader;
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
        addSerializer(ExternalTerminalType.class, new ExternalTerminalTypeSerializer());
        addDeserializer(ExternalTerminalType.class, new ExternalTerminalTypeDeserializer());
        addSerializer(EncryptedValue.class, new EncryptedValueSerializer());
        addDeserializer(EncryptedValue.class, new EncryptedValueDeserializer<>());
        addSerializer(EncryptedValue.CurrentKey.class, new EncryptedValueSerializer());
        addDeserializer(EncryptedValue.CurrentKey.class, new EncryptedValueDeserializer<>());
        addSerializer(EncryptedValue.VaultKey.class, new EncryptedValueSerializer());
        addDeserializer(EncryptedValue.VaultKey.class, new EncryptedValueDeserializer<>());

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

    @SuppressWarnings("all")
    public static class EncryptedValueSerializer extends JsonSerializer<EncryptedValue> {

        @Override
        public void serialize(EncryptedValue value, JsonGenerator jgen, SerializerProvider provider)
                throws IOException {
            if (value.getValue() == null) {
                jgen.writeNull();
                return;
            }

            jgen.writeTree(value.getSecret().serialize(value.allowUserSecretKey()));
        }

        @Override
        public void serializeWithType(
                EncryptedValue value, JsonGenerator gen, SerializerProvider serializers, TypeSerializer typeSer)
                throws IOException {
            if (value.getValue() == null) {
                gen.writeNull();
                return;
            }

            gen.writeTree(value.getSecret().serialize(value.allowUserSecretKey()));
        }
    }

    @SuppressWarnings("all")
    public static class EncryptedValueDeserializer<T extends EncryptedValue<?>> extends JsonDeserializer<T>
            implements ContextualDeserializer {

        private boolean useCurrentSecretKeyIfPossible;
        private boolean forceCurrentSecretKey;
        private Class<?> type;

        @Override
        @SuppressWarnings("unchecked")
        public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property)
                throws JsonMappingException {
            var deserializer = new EncryptedValueDeserializer();
            if (property == null) {
                return deserializer;
            }

            JavaType wrapperType = property.getType();
            JavaType valueType = wrapperType.containedType(0);
            deserializer.useCurrentSecretKeyIfPossible =
                    !wrapperType.getRawClass().equals(EncryptedValue.VaultKey.class);
            deserializer.forceCurrentSecretKey = wrapperType.getRawClass().equals(EncryptedValue.CurrentKey.class);
            deserializer.type = valueType.getRawClass();
            return deserializer;
        }

        @Override
        @SuppressWarnings("unchecked")
        public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            if (type == null) {
                return null;
            }

            return (T) get(p, type, useCurrentSecretKeyIfPossible, forceCurrentSecretKey);
        }

        public Object deserializeWithType(JsonParser jp, DeserializationContext ctxt, TypeDeserializer typeDeserializer)
                throws IOException {
            var type = ((AsPropertyTypeDeserializer) typeDeserializer).baseType();
            JavaType wrapperType = type;
            JavaType valueType = wrapperType.containedType(0);
            var useCurrentSecretKey = !wrapperType.equals(SimpleType.constructUnsafe(EncryptedValue.VaultKey.class));
            var forceCurrentSecretKey = wrapperType.equals(SimpleType.constructUnsafe(EncryptedValue.CurrentKey.class));
            return get(jp, valueType.getRawClass(), useCurrentSecretKey, forceCurrentSecretKey);
        }

        private EncryptedValue get(
                JsonParser p, Class<?> type, boolean useCurrentSecretKey, boolean forceCurrentSecretKey)
                throws IOException {
            if (forceCurrentSecretKey && DataStorageUserHandler.getInstance().getActiveUser() == null) {
                return null;
            }

            Object value;
            JsonNode tree = JacksonMapper.getDefault().readTree(p);
            var secret = DataStorageSecret.deserialize(tree);
            if (secret == null) {
                var raw = JacksonMapper.getDefault().treeToValue(tree, type);
                if (raw != null) {
                    value = raw;
                    var s = JacksonMapper.getDefault().writeValueAsString(value);
                    var internalSecret = InPlaceSecretValue.of(s.toCharArray());
                    secret = DataStorageSecret.ofSecret(
                            internalSecret,
                            useCurrentSecretKey
                                            && DataStorageUserHandler.getInstance()
                                                            .getActiveUser()
                                                    != null
                                    ? EncryptionToken.ofUser()
                                    : EncryptionToken.ofVaultKey());
                } else {
                    return null;
                }
            } else {
                if (!secret.getEncryptedToken().canDecrypt()) {
                    return null;
                }

                var s = secret.getSecret();
                if (s.length == 0) {
                    return null;
                }
                value = JacksonMapper.getDefault().readValue(new CharArrayReader(s), type);
            }
            var perUser = useCurrentSecretKey;
            return perUser
                    ? new EncryptedValue.CurrentKey<>(value, secret)
                    : new EncryptedValue.VaultKey<>(value, secret);
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
            jgen.writeString(value.get().getUuid().toString());
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
                            || !dataStoreEntry.getStoreNode().isReadableForUser())
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
