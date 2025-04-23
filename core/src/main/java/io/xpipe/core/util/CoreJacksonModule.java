package io.xpipe.core.util;

import io.xpipe.core.dialog.BaseQueryElement;
import io.xpipe.core.dialog.BusyElement;
import io.xpipe.core.dialog.ChoiceElement;
import io.xpipe.core.dialog.HeaderElement;
import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellDialect;
import io.xpipe.core.process.ShellDialects;
import io.xpipe.core.process.ShellScript;
import io.xpipe.core.store.FilePath;
import io.xpipe.core.store.StorePath;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public class CoreJacksonModule extends SimpleModule {

    @Override
    public void setupModule(SetupContext context) {
        context.registerSubtypes(
                new NamedType(InPlaceSecretValue.class),
                new NamedType(BaseQueryElement.class),
                new NamedType(ChoiceElement.class),
                new NamedType(BusyElement.class),
                new NamedType(HeaderElement.class));

        for (ShellDialect t : ShellDialects.ALL) {
            context.registerSubtypes(new NamedType(t.getClass()));
        }

        addSerializer(FilePath.class, new FilePathSerializer());
        addDeserializer(FilePath.class, new FilePathDeserializer());

        addSerializer(StorePath.class, new StorePathSerializer());
        addDeserializer(StorePath.class, new StorePathDeserializer());

        addSerializer(Charset.class, new CharsetSerializer());
        addDeserializer(Charset.class, new CharsetDeserializer());

        addSerializer(ShellDialect.class, new ShellDialectSerializer());
        addDeserializer(ShellDialect.class, new ShellDialectDeserializer());

        addSerializer(StreamCharset.class, new StreamCharsetSerializer());
        addDeserializer(StreamCharset.class, new StreamCharsetDeserializer());

        addSerializer(NewLine.class, new NewLineSerializer());
        addDeserializer(NewLine.class, new NewLineDeserializer());

        addSerializer(Path.class, new LocalPathSerializer());
        addDeserializer(Path.class, new LocalPathDeserializer());

        addSerializer(OsType.class, new OsTypeSerializer());
        addDeserializer(OsType.Local.class, new OsTypeLocalDeserializer());
        addDeserializer(OsType.Any.class, new OsTypeAnyDeserializer());

        addSerializer(ShellScript.class, new ShellScriptSerializer());
        addDeserializer(ShellScript.class, new ShellScriptDeserializer());

        context.setMixInAnnotations(Throwable.class, ThrowableTypeMixIn.class);

        context.addSerializers(_serializers);
        context.addDeserializers(_deserializers);
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

    public static class StorePathSerializer extends JsonSerializer<StorePath> {

        @Override
        public void serialize(StorePath value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            var ar = value.getNames().toArray(String[]::new);
            jgen.writeArray(ar, 0, ar.length);
        }
    }

    public static class StorePathDeserializer extends JsonDeserializer<StorePath> {

        @Override
        public StorePath deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JavaType javaType =
                    JacksonMapper.getDefault().getTypeFactory().constructCollectionLikeType(List.class, String.class);
            List<String> list = JacksonMapper.getDefault().readValue(p, javaType);
            return new StorePath(list);
        }
    }

    public static class FilePathSerializer extends JsonSerializer<FilePath> {

        @Override
        public void serialize(FilePath value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            jgen.writeString(value.toString());
        }
    }

    public static class FilePathDeserializer extends JsonDeserializer<FilePath> {

        @Override
        public FilePath deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            return FilePath.of(p.getValueAsString());
        }
    }

    public static class CharsetSerializer extends JsonSerializer<Charset> {

        @Override
        public void serialize(Charset value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            jgen.writeString(value.name());
        }
    }

    public static class CharsetDeserializer extends JsonDeserializer<Charset> {

        @Override
        public Charset deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            return Charset.forName(p.getValueAsString());
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

    public static class NewLineSerializer extends JsonSerializer<NewLine> {

        @Override
        public void serialize(NewLine value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            jgen.writeString(value.getId());
        }
    }

    public static class NewLineDeserializer extends JsonDeserializer<NewLine> {

        @Override
        public NewLine deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            return NewLine.byId(p.getValueAsString());
        }
    }

    public static class StreamCharsetSerializer extends JsonSerializer<StreamCharset> {

        @Override
        public void serialize(StreamCharset value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            jgen.writeString(value.toString());
        }
    }

    public static class StreamCharsetDeserializer extends JsonDeserializer<StreamCharset> {

        @Override
        public StreamCharset deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            return StreamCharset.get(p.getValueAsString());
        }
    }

    public static class LocalPathSerializer extends JsonSerializer<Path> {

        @Override
        public void serialize(Path value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            jgen.writeString(value.toString());
        }
    }

    public static class LocalPathDeserializer extends JsonDeserializer<Path> {

        @Override
        public Path deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            return Path.of(p.getValueAsString());
        }
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

    @JsonSerialize(as = Throwable.class)
    public abstract static class ThrowableTypeMixIn {

        @JsonIdentityInfo(generator = ObjectIdGenerators.StringIdGenerator.class, property = "$id")
        private Throwable cause;
    }
}
