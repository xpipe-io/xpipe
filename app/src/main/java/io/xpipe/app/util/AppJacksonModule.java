package io.xpipe.app.util;

import io.xpipe.app.beacon.BeaconAuthMethod;
import io.xpipe.app.beacon.BeaconClientInformation;
import io.xpipe.app.process.ShellDialect;
import io.xpipe.app.process.ShellDialects;
import io.xpipe.app.process.ShellScript;
import io.xpipe.app.rdp.ExternalRdpClient;
import io.xpipe.app.secret.*;
import io.xpipe.app.spice.ExternalSpiceClient;
import io.xpipe.app.storage.*;
import io.xpipe.app.terminal.ExternalTerminalType;
import io.xpipe.app.terminal.TerminalMultiplexer;
import io.xpipe.app.terminal.TerminalPrompt;
import io.xpipe.app.vnc.ExternalVncClient;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.TokenStreamLocation;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.jsontype.NamedType;
import tools.jackson.databind.jsontype.TypeDeserializer;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.jsontype.impl.AsPropertyTypeDeserializer;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.node.JsonNodeFactory;

import java.io.CharArrayReader;
import java.nio.charset.Charset;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AppJacksonModule extends SimpleModule {

    @Override
    public void setupModule(SetupContext context) {
        // Load this class early to prevent weird StackOverflow issues
        // when Jackson loads this class itself
        var _ = TokenStreamLocation.NA;

        registerSubtypes(
                new NamedType(BeaconClientInformation.Api.class),
                new NamedType(BeaconClientInformation.Cli.class),
                new NamedType(BeaconClientInformation.Daemon.class));
        registerSubtypes(new NamedType(BeaconAuthMethod.Local.class), new NamedType(BeaconAuthMethod.ApiKey.class));

        registerSubtypes(InPlaceSecretValue.class);
        registerSubtypes(PrincipalSecretValue.class);

        for (ShellDialect t : ShellDialects.ALL) {
            registerSubtypes(new NamedType(t.getClass()));
        }

        registerSubtypes(TerminalMultiplexer.getClasses());
        registerSubtypes(TerminalPrompt.getClasses());
        registerSubtypes(ExternalVncClient.getClasses());
        registerSubtypes(ExternalRdpClient.getClasses());
        registerSubtypes(ExternalSpiceClient.getClasses());
        registerSubtypes(SecretRetrievalStrategy.getClasses());

        addSerializer(InPlaceSecretValue.class, new InPlaceSecretValueSerializer());
        addDeserializer(InPlaceSecretValue.class, new InPlaceSecretValueDeserializer());

        addSerializer(DataStoreEntryRef.class, new DataStoreEntryRefSerializer());
        addDeserializer(DataStoreEntryRef.class, new DataStoreEntryRefDeserializer());

        addSerializer(ContextualFileReference.class, new LocalFileReferenceSerializer());
        addDeserializer(ContextualFileReference.class, new LocalFileReferenceDeserializer());

        addSerializer(ExternalTerminalType.class, new ExternalTerminalTypeSerializer());
        addDeserializer(ExternalTerminalType.class, new ExternalTerminalTypeDeserializer());

        addSerializer(EncryptedValue.class, new EncryptedValueSerializer());
        addDeserializer(EncryptedValue.class, new EncryptedValueDeserializer<>());

        addSerializer(ShellDialect.class, new ShellDialectSerializer());
        addDeserializer(ShellDialect.class, new ShellDialectDeserializer());

        addSerializer(OsType.class, new OsTypeSerializer());
        addDeserializer(OsType.Local.class, new OsTypeLocalDeserializer());
        addDeserializer(OsType.Any.class, new OsTypeAnyDeserializer());

        addSerializer(ShellScript.class, new ShellScriptSerializer());
        addDeserializer(ShellScript.class, new ShellScriptDeserializer());

        addSerializer(DataStoreAccessScope.class, new DataStoreAccessScopeSerializer());
        addDeserializer(DataStoreAccessScope.class, new DataStoreAccessScopeDeserializer());

        addSerializer(FilePath.class, new FilePathSerializer());
        addDeserializer(FilePath.class, new FilePathDeserializer());

        addSerializer(StorePath.class, new StorePathSerializer());
        addDeserializer(StorePath.class, new StorePathDeserializer());

        addSerializer(Charset.class, new CharsetSerializer());
        addDeserializer(Charset.class, new CharsetDeserializer());

        addSerializer(Path.class, new LocalPathSerializer());
        addDeserializer(Path.class, new LocalPathDeserializer());

        addSerializer(EncryptionToken.class, new EncryptionTokenSerializer());
        addDeserializer(EncryptionToken.class, new EncryptionTokenDeserializer());

        addSerializer(HostAddress.class, new HostAddressSerializer());
        addDeserializer(HostAddress.class, new HostAddressDeserializer());

        setMixInAnnotation(Throwable.class, ThrowableTypeMixIn.class);

        super.setupModule(context);
    }

    public static class InPlaceSecretValueSerializer extends ValueSerializer<InPlaceSecretValue> {

        @Override
        public void serializeWithType(
                InPlaceSecretValue value, JsonGenerator gen, SerializationContext ctxt, TypeSerializer typeSer)
                throws JacksonException {
            serialize(value, gen, ctxt);
        }

        @Override
        public void serialize(InPlaceSecretValue value, JsonGenerator jgen, SerializationContext context) {
            if (value == null) {
                jgen.writeNull();
                return;
            }

            var tree = JsonNodeFactory.instance.objectNode();
            tree.put("type", "internal");
            tree.put("encryptedValue", value.getEncryptedValue());
            jgen.writeTree(tree);
        }
    }

    public static class InPlaceSecretValueDeserializer extends ValueDeserializer<InPlaceSecretValue> {

        @Override
        public Object deserializeWithType(JsonParser p, DeserializationContext ctxt, TypeDeserializer typeDeserializer)
                throws JacksonException {
            return deserialize(p, ctxt);
        }

        @Override
        public InPlaceSecretValue deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
            JsonNode tree = JacksonMapper.getDefault().readTree(p);
            if (tree.isString()) {
                return InPlaceSecretValue.of(tree.stringValue());
            }

            var enc = tree.get("encryptedValue");
            if (enc == null) {
                return null;
            }

            var type = tree.get("type");
            if (type != null && !type.asString().equals("internal")) {
                return null;
            }

            return InPlaceSecretValue.builder()
                    .encryptedValue(enc.stringValue())
                    .build();
        }
    }

    public static class EncryptionTokenSerializer extends ValueSerializer<EncryptionToken> {

        @Override
        public void serialize(EncryptionToken value, JsonGenerator jgen, SerializationContext context) {
            jgen.writeString(value.getToken());
        }
    }

    public static class EncryptionTokenDeserializer extends ValueDeserializer<EncryptionToken> {

        @Override
        public EncryptionToken deserialize(JsonParser p, DeserializationContext ctxt) {
            var s = p.getValueAsString();
            return s != null ? EncryptionToken.builder().token(s).build() : null;
        }
    }

    public static class DataStoreAccessScopeSerializer extends ValueSerializer<DataStoreAccessScope> {

        @Override
        public void serialize(DataStoreAccessScope value, JsonGenerator jgen, SerializationContext context) {
            var node = JacksonMapper.getDefault().valueToTree(value.getPrincipals());
            jgen.writeTree(node);
        }
    }

    public static class DataStoreAccessScopeDeserializer extends ValueDeserializer<DataStoreAccessScope> {

        @Override
        public DataStoreAccessScope deserialize(JsonParser p, DeserializationContext ctxt) {
            var principals = JacksonMapper.getDefault()
                    .treeToValue(p.readValueAsTree(), new TypeReference<Set<EncryptionPrincipal>>() {});
            var valid = principals.stream()
                    .filter(encryptionPrincipal -> encryptionPrincipal != null)
                    .collect(Collectors.toSet());
            return !valid.isEmpty() ? DataStoreAccessScope.of(valid) : null;
        }
    }

    public static class StorePathSerializer extends ValueSerializer<StorePath> {

        @Override
        public void serialize(StorePath value, JsonGenerator jgen, SerializationContext context) {
            var ar = value.getNames().toArray(String[]::new);
            jgen.writeArray(ar, 0, ar.length);
        }
    }

    public static class StorePathDeserializer extends ValueDeserializer<StorePath> {

        @Override
        public StorePath deserialize(JsonParser p, DeserializationContext ctxt) {
            JavaType javaType =
                    JacksonMapper.getDefault().getTypeFactory().constructCollectionLikeType(List.class, String.class);
            List<String> list = JacksonMapper.getDefault().readValue(p, javaType);
            return new StorePath(list);
        }
    }

    public static class FilePathSerializer extends ValueSerializer<FilePath> {

        @Override
        public void serialize(FilePath value, JsonGenerator jgen, SerializationContext context) {
            jgen.writeString(value.toString());
        }
    }

    public static class FilePathDeserializer extends ValueDeserializer<FilePath> {

        @Override
        public FilePath deserialize(JsonParser p, DeserializationContext ctxt) {
            return FilePath.of(p.getValueAsString());
        }
    }

    public static class CharsetSerializer extends ValueSerializer<Charset> {

        @Override
        public void serialize(Charset value, JsonGenerator jgen, SerializationContext context) {
            jgen.writeString(value.name());
        }
    }

    public static class CharsetDeserializer extends ValueDeserializer<Charset> {

        @Override
        public Charset deserialize(JsonParser p, DeserializationContext ctxt) {
            return Charset.forName(p.getValueAsString());
        }
    }

    public static class LocalPathSerializer extends ValueSerializer<Path> {

        @Override
        public void serialize(Path value, JsonGenerator jgen, SerializationContext context) {
            jgen.writeString(value.toString());
        }
    }

    public static class LocalPathDeserializer extends ValueDeserializer<Path> {

        @Override
        public Path deserialize(JsonParser p, DeserializationContext ctxt) {
            try {
                return Path.of(p.getValueAsString());
            } catch (InvalidPathException ignored) {
                return null;
            }
        }
    }

    @JsonSerialize(as = Throwable.class)
    @JsonPropertyOrder(alphabetic = true)
    public abstract static class ThrowableTypeMixIn {

        @SuppressWarnings("unused")
        @JsonIdentityInfo(generator = ObjectIdGenerators.StringIdGenerator.class, property = "$id")
        private Throwable cause;
    }

    public static class OsTypeSerializer extends ValueSerializer<OsType> {

        @Override
        public void serialize(OsType value, JsonGenerator jgen, SerializationContext context) {
            jgen.writeString(value.getId());
        }
    }

    public static class OsTypeLocalDeserializer extends ValueDeserializer<OsType.Local> {

        @Override
        public OsType.Local deserialize(JsonParser p, DeserializationContext ctxt) {
            var stream = Stream.of(OsType.WINDOWS, OsType.LINUX, OsType.MACOS);
            var n = p.getValueAsString();
            return stream.filter(osType ->
                            osType.getName().equals(n) || osType.getId().equals(n))
                    .findFirst()
                    .orElse(null);
        }
    }

    public static class OsTypeAnyDeserializer extends ValueDeserializer<OsType.Any> {

        @Override
        public OsType.Any deserialize(JsonParser p, DeserializationContext ctxt) {
            var stream = Stream.of(
                    OsType.WINDOWS, OsType.LINUX, OsType.BSD, OsType.SOLARIS, OsType.MACOS, OsType.AIX, OsType.UNIX);
            var n = p.getValueAsString();
            return stream.filter(osType ->
                            osType.getName().equals(n) || osType.getId().equals(n))
                    .findFirst()
                    .orElse(null);
        }
    }

    public static class LocalFileReferenceSerializer extends ValueSerializer<ContextualFileReference> {

        @Override
        public void serialize(ContextualFileReference value, JsonGenerator jgen, SerializationContext context) {
            jgen.writeString(value.serialize());
        }
    }

    public static class ShellDialectSerializer extends ValueSerializer<ShellDialect> {

        @Override
        public void serialize(ShellDialect value, JsonGenerator jgen, SerializationContext context) {
            jgen.writeString(value.getId());
        }
    }

    public static class ShellDialectDeserializer extends ValueDeserializer<ShellDialect> {

        @Override
        public ShellDialect deserialize(JsonParser p, DeserializationContext ctxt) {
            JsonNode tree = JacksonMapper.getDefault().readTree(p);
            if (tree.isObject()) {
                var t = tree.get("type");
                if (t == null) {
                    return null;
                }
                return ShellDialects.byIdIfPresent(t.asString()).orElse(null);
            }

            return ShellDialects.byIdIfPresent(tree.asString()).orElse(null);
        }
    }

    public static class ShellScriptSerializer extends ValueSerializer<ShellScript> {

        @Override
        public void serialize(ShellScript value, JsonGenerator jgen, SerializationContext context) {
            jgen.writeString(value.getValue());
        }
    }

    public static class ShellScriptDeserializer extends ValueDeserializer<ShellScript> {

        @Override
        public ShellScript deserialize(JsonParser p, DeserializationContext ctxt) {
            return new ShellScript(p.getValueAsString());
        }
    }

    public static class LocalFileReferenceDeserializer extends ValueDeserializer<ContextualFileReference> {

        @Override
        public ContextualFileReference deserialize(JsonParser p, DeserializationContext ctxt) {
            return ContextualFileReference.of(p.getValueAsString());
        }
    }

    public static class ExternalTerminalTypeSerializer extends ValueSerializer<ExternalTerminalType> {

        @Override
        public void serialize(ExternalTerminalType value, JsonGenerator gen, SerializationContext ctxt)
                throws JacksonException {
            gen.writeString(value.getId());
        }
    }

    public static class ExternalTerminalTypeDeserializer extends ValueDeserializer<ExternalTerminalType> {

        @Override
        public ExternalTerminalType deserialize(JsonParser p, DeserializationContext ctxt) {
            var id = p.getValueAsString();
            return ExternalTerminalType.ALL_ON_ALL_PLATFORMS.stream()
                    .filter(terminalType -> terminalType.getId().equals(id))
                    .findFirst()
                    .orElse(null);
        }
    }

    @SuppressWarnings("all")
    public static class EncryptedValueSerializer extends ValueSerializer<EncryptedValue> {

        @Override
        public void serialize(EncryptedValue value, JsonGenerator jgen, SerializationContext context) {
            if (!value.isEncrypted()) {
                jgen.writeTree(JacksonMapper.getDefault().valueToTree(value.getValue()));
                return;
            }

            jgen.writeTree(value.getSecret().serialize());
        }

        @Override
        public void serializeWithType(
                EncryptedValue value, JsonGenerator gen, SerializationContext context, TypeSerializer typeSer) {
            serialize(value, gen, context);
        }
    }

    @SuppressWarnings("all")
    public static class EncryptedValueDeserializer<T extends EncryptedValue<?>> extends ValueDeserializer<T> {

        private Class<?> type;

        @Override
        @SuppressWarnings("unchecked")
        public ValueDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) {
            var deserializer = new EncryptedValueDeserializer();
            if (property == null && ctxt.getContextualType() == null) {
                return deserializer;
            }

            JavaType wrapperType = property != null ? property.getType() : ctxt.getContextualType();
            JavaType valueType = wrapperType.containedType(0);
            deserializer.type = valueType.getRawClass();
            return deserializer;
        }

        @Override
        @SuppressWarnings("unchecked")
        public T deserialize(JsonParser p, DeserializationContext ctxt) {
            if (type == null) {
                return null;
            }

            return (T) get(p, type);
        }

        @SuppressWarnings("unchecked")
        public Object deserializeWithType(
                JsonParser jp, DeserializationContext ctxt, TypeDeserializer typeDeserializer) {
            var type = ((AsPropertyTypeDeserializer) typeDeserializer).baseType();
            JavaType wrapperType = type;
            JavaType valueType = wrapperType.containedType(0);
            return get(jp, valueType.getRawClass());
        }

        @SuppressWarnings("unchecked")
        private EncryptedValue get(JsonParser p, Class<?> type) {
            JsonNode tree = JacksonMapper.getDefault().readTree(p);
            var encrypted = DataStorageSecret.matches(tree);
            if (encrypted) {
                var storageSecret = DataStorageSecret.deserialize(tree);
                if (storageSecret == null) {
                    return null;
                }

                var secret = storageSecret.getInternalSecret();
                var valueJson = secret != null
                        ? JacksonMapper.getDefault().readTree(new CharArrayReader(secret.getSecret()))
                        : null;
                var value = valueJson != null ? JacksonMapper.getDefault().treeToValue(valueJson, type) : null;
                return new EncryptedValue(valueJson, value, storageSecret, true);
            } else {
                var val = JacksonMapper.getDefault().treeToValue(tree, type);
                return val != null ? new EncryptedValue(tree, val, null, false) : null;
            }
        }
    }

    @SuppressWarnings("all")
    public static class DataStoreEntryRefSerializer extends ValueSerializer<DataStoreEntryRef> {

        @Override
        public void serialize(DataStoreEntryRef value, JsonGenerator jgen, SerializationContext context) {
            if (value == null) {
                jgen.writeNull();
                return;
            }

            jgen.writeString(value.get().getUuid().toString());
        }
    }

    public static class DataStoreEntryRefDeserializer extends ValueDeserializer<DataStoreEntryRef<?>> {

        @Override
        public DataStoreEntryRef<?> deserialize(JsonParser p, DeserializationContext ctxt) {
            JsonNode tree = p.objectReadContext().readTree(p);
            if (tree == null || !tree.isString()) {
                return null;
            }

            var text = tree.stringValue();
            var id = UUID.fromString(text);
            // Keep an invalid entry if it is per-user, meaning that it will get removed later on
            var e = DataStorage.get()
                    .getStoreEntryIfPresent(id)
                    .filter(dataStoreEntry -> dataStoreEntry.getValidity() != DataStoreEntry.Validity.LOAD_FAILED
                            || (dataStoreEntry.getStoreNode() != null
                                    && !dataStoreEntry.getStoreNode().isAccessible()))
                    .orElse(null);
            if (e == null) {
                return null;
            }

            return new DataStoreEntryRef<>(e);
        }
    }

    public static class HostAddressSerializer extends ValueSerializer<HostAddress> {

        @Override
        public void serialize(HostAddress value, JsonGenerator jgen, SerializationContext context) {
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

    public static class HostAddressDeserializer extends ValueDeserializer<HostAddress> {

        @Override
        public HostAddress deserialize(JsonParser p, DeserializationContext ctxt) {
            var tree = (JsonNode) p.readValueAsTree();
            if (tree.isString()) {
                return !tree.stringValue().isBlank() ? HostAddress.of(tree.stringValue()) : null;
            } else {
                var value = tree.get("value");
                var available = tree.get("available");
                if (value == null || !value.isString() || available == null || !available.isArray()) {
                    return null;
                }

                var l = new ArrayList<String>();
                for (JsonNode jsonNode : available) {
                    l.add(jsonNode.stringValue());
                }
                return HostAddress.of(value.stringValue(), l);
            }
        }
    }
}
