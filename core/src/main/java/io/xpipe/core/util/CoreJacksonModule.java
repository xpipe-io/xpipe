package io.xpipe.core.util;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.type.ArrayType;
import io.xpipe.core.charsetter.NewLine;
import io.xpipe.core.charsetter.StreamCharset;
import io.xpipe.core.dialog.BaseQueryElement;
import io.xpipe.core.dialog.BusyElement;
import io.xpipe.core.dialog.ChoiceElement;
import io.xpipe.core.dialog.HeaderElement;
import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellDialect;
import io.xpipe.core.process.ShellDialects;
import io.xpipe.core.store.LocalStore;
import io.xpipe.core.store.StdinDataStore;
import io.xpipe.core.store.StdoutDataStore;

import java.io.IOException;
import java.lang.reflect.WildcardType;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.stream.Stream;

public class CoreJacksonModule extends SimpleModule {

    @Override
    public void setupModule(SetupContext context) {
        context.registerSubtypes(
                new NamedType(DefaultSecretValue.class),
                new NamedType(StdinDataStore.class),
                new NamedType(StdoutDataStore.class),
                new NamedType(LocalStore.class),
                new NamedType(ArrayType.class),
                new NamedType(WildcardType.class),
                new NamedType(BaseQueryElement.class),
                new NamedType(ChoiceElement.class),
                new NamedType(BusyElement.class),
                new NamedType(HeaderElement.class));

        for (ShellDialect t : ShellDialects.ALL) {
            context.registerSubtypes(new NamedType(t.getClass()));
        }

        addSerializer(Charset.class, new CharsetSerializer());
        addDeserializer(Charset.class, new CharsetDeserializer());

        addSerializer(StreamCharset.class, new StreamCharsetSerializer());
        addDeserializer(StreamCharset.class, new StreamCharsetDeserializer());

        addSerializer(NewLine.class, new NewLineSerializer());
        addDeserializer(NewLine.class, new NewLineDeserializer());

        addSerializer(Path.class, new LocalPathSerializer());
        addDeserializer(Path.class, new LocalPathDeserializer());

        addSerializer(OsType.class, new OsTypeSerializer());
        addDeserializer(OsType.class, new OsTypeDeserializer());

        context.setMixInAnnotations(Throwable.class, ThrowableTypeMixIn.class);

        context.addSerializers(_serializers);
        context.addDeserializers(_deserializers);
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

    public static class OsTypeDeserializer extends JsonDeserializer<OsType> {

        @Override
        public OsType deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            var n = p.getValueAsString();
            return Stream.of(OsType.WINDOWS, OsType.LINUX, OsType.MACOS).filter(osType -> osType.getName().equals(n)).findFirst().orElse(null);
        }
    }

    @JsonSerialize(as = Throwable.class)
    public abstract static class ThrowableTypeMixIn {

        @JsonIdentityInfo(generator = ObjectIdGenerators.StringIdGenerator.class, property = "$id")
        private Throwable cause;
    }
}
