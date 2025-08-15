package io.xpipe.core;

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
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;

public class CoreJacksonModule extends SimpleModule {

    @Override
    public void setupModule(SetupContext context) {
        context.registerSubtypes(new NamedType(InPlaceSecretValue.class));

        addSerializer(FilePath.class, new FilePathSerializer());
        addDeserializer(FilePath.class, new FilePathDeserializer());

        addSerializer(StorePath.class, new StorePathSerializer());
        addDeserializer(StorePath.class, new StorePathDeserializer());

        addSerializer(Charset.class, new CharsetSerializer());
        addDeserializer(Charset.class, new CharsetDeserializer());

        addSerializer(Path.class, new LocalPathSerializer());
        addDeserializer(Path.class, new LocalPathDeserializer());

        context.setMixInAnnotations(Throwable.class, ThrowableTypeMixIn.class);

        context.addSerializers(_serializers);
        context.addDeserializers(_deserializers);
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

    public static class LocalPathSerializer extends JsonSerializer<Path> {

        @Override
        public void serialize(Path value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            jgen.writeString(value.toString());
        }
    }

    public static class LocalPathDeserializer extends JsonDeserializer<Path> {

        @Override
        public Path deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            try {
                return Path.of(p.getValueAsString());
            } catch (InvalidPathException ignored) {
                return null;
            }
        }
    }

    @JsonSerialize(as = Throwable.class)
    public abstract static class ThrowableTypeMixIn {

        @JsonIdentityInfo(generator = ObjectIdGenerators.StringIdGenerator.class, property = "$id")
        private Throwable cause;
    }
}
