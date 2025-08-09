package io.xpipe.app.util;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.xpipe.app.ext.HostAddress;
import io.xpipe.app.process.ShellDialect;
import io.xpipe.app.process.ShellDialects;
import io.xpipe.app.process.ShellScript;
import io.xpipe.app.pwman.PasswordManager;
import io.xpipe.app.storage.*;
import io.xpipe.app.terminal.ExternalTerminalType;
import io.xpipe.app.terminal.TerminalMultiplexer;
import io.xpipe.app.terminal.TerminalPrompt;
import io.xpipe.app.vnc.ExternalVncClient;
import io.xpipe.core.InPlaceSecretValue;
import io.xpipe.core.JacksonMapper;
import io.xpipe.core.OsType;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.jsontype.impl.AsPropertyTypeDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.type.SimpleType;

import java.io.CharArrayReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;
import java.util.stream.Stream;

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

        addSerializer(ShellDialect.class, new ShellDialectSerializer());
        addDeserializer(ShellDialect.class, new ShellDialectDeserializer());

        addSerializer(OsType.class, new OsTypeSerializer());
        addDeserializer(OsType.Local.class, new OsTypeLocalDeserializer());
        addDeserializer(OsType.Any.class, new OsTypeAnyDeserializer());

        addSerializer(ShellScript.class, new ShellScriptSerializer());
        addDeserializer(ShellScript.class, new ShellScriptDeserializer());

        addSerializer(HostAddress.class, new HostAddressSerializer());
        addDeserializer(HostAddress.class, new HostAddressDeserializer());

        for (ShellDialect t : ShellDialects.ALL) {
            context.registerSubtypes(new NamedType(t.getClass()));
        }

        context.registerSubtypes(PasswordManager.getClasses());
        context.registerSubtypes(TerminalMultiplexer.getClasses());
        context.registerSubtypes(TerminalPrompt.getClasses());
        context.registerSubtypes(ExternalVncClient.getClasses());

        context.addSerializers(_serializers);
        context.addDeserializers(_deserializers);
    }

    public static class OsTypeSerializer extends JsonSerializer<OsType> {

        @Override
        public void serialize(OsType value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            jgen.writeString(value.getName());
        }
    }

    public static class OsTypeLocalDeserializer extends JsonDeserializer<OsType.Local> {

        @Override
        public OsType.Local deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            var stream = Stream.of(OsType.WINDOWS, OsType.LINUX, OsType.MACOS);
            var n = p.getValueAsString();
            return stream.filter(osType -> osType.getName().equals(n))
                    .findFirst()
                    .orElse(null);
        }
    }

    public static class OsTypeAnyDeserializer extends JsonDeserializer<OsType.Any> {

        @Override
        public OsType.Any deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            var stream = Stream.of(OsType.WINDOWS, OsType.LINUX, OsType.BSD, OsType.SOLARIS, OsType.MACOS);
            var n = p.getValueAsString();
            return stream.filter(osType -> osType.getName().equals(n))
                    .findFirst()
                    .orElse(null);
        }
    }

    public static class LocalFileReferenceSerializer extends JsonSerializer<ContextualFileReference> {

        @Override
        public void serialize(ContextualFileReference value, JsonGenerator jgen, SerializerProvider provider)
                throws IOException {
            jgen.writeString(value.serialize());
        }
    }

    public static class ShellDialectSerializer extends JsonSerializer<ShellDialect> {

        @Override
        public void serialize(ShellDialect value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            jgen.writeString(value.getId());
        }
    }

    public static class ShellDialectDeserializer extends JsonDeserializer<ShellDialect> {

        @Override
        public ShellDialect deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode tree = JacksonMapper.getDefault().readTree(p);
            if (tree.isObject()) {
                var t = tree.get("type");
                if (t == null) {
                    return null;
                }
                return ShellDialects.byNameIfPresent(t.asText()).orElse(null);
            }

            return ShellDialects.byNameIfPresent(tree.asText()).orElse(null);
        }
    }

    public static class ShellScriptSerializer extends JsonSerializer<ShellScript> {

        @Override
        public void serialize(ShellScript value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            jgen.writeString(value.getValue());
        }
    }

    public static class ShellScriptDeserializer extends JsonDeserializer<ShellScript> {

        @Override
        public ShellScript deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            return new ShellScript(p.getValueAsString());
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
                if (value == null) {
                    return null;
                }
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

            jgen.writeString(value.get().getUuid().toString());
        }
    }

    public static class DataStoreEntryRefDeserializer extends JsonDeserializer<DataStoreEntryRef<?>> {

        @Override
        public DataStoreEntryRef<?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode tree = p.getCodec().readTree(p);
            if (tree == null) {
                return null;
            }

            String text;
            if (tree.isObject()) {
                var obj = (ObjectNode) tree;
                if (!obj.has("storeId") || !obj.required("storeId").isTextual()) {
                    return null;
                }

                text = obj.required("storeId").asText();
                if (text.isBlank()) {
                    return null;
                }
            } else {
                if (!tree.isTextual()) {
                    return null;
                }
                text = tree.asText();
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

            return new DataStoreEntryRef<>(e);
        }
    }

    public static class HostAddressSerializer extends JsonSerializer<HostAddress> {





        @Override


        public void serialize(HostAddress value, JsonGenerator jgen, SerializerProvider provider)
                throws IOException {
            if (value.isSingle()) {
                jgen.writeString(value.get());
            } else {
                var tree = JsonNodeFactory.instance.objectNode();
                tree.put("value", value.get());
                tree.set("available", JacksonMapper.getDefault().valueToTree(value.getAvailable()));
                jgen.writeTree(tree);
            }
        }
    }





    public static class HostAddressDeserializer extends JsonDeserializer<HostAddress> {





        @Override


        public HostAddress deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            var tree = (JsonNode) p.getCodec().readTree(p);
            if (tree.isTextual()) {
                return HostAddress.of(tree.textValue());
            } else {
                var value = tree.get("value");
                var available = tree.get("available");
                if (value == null || !value.isTextual() || available == null || !available.isArray()) {
                    return null;
                }

                var l = new ArrayList<String>();
                for (JsonNode jsonNode : available) {
                    l.add(jsonNode.textValue());
                }
                return HostAddress.of(value.textValue(), l);
            }
        }
    }
}
