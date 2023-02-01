package io.xpipe.core.util;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.xpipe.core.charsetter.NewLine;
import io.xpipe.core.charsetter.StreamCharset;
import io.xpipe.core.data.type.ArrayType;
import io.xpipe.core.data.type.TupleType;
import io.xpipe.core.data.type.ValueType;
import io.xpipe.core.data.type.WildcardType;
import io.xpipe.core.dialog.BaseQueryElement;
import io.xpipe.core.dialog.BusyElement;
import io.xpipe.core.dialog.ChoiceElement;
import io.xpipe.core.dialog.HeaderElement;
import io.xpipe.core.impl.*;
import io.xpipe.core.process.ShellType;
import io.xpipe.core.process.ShellTypes;
import io.xpipe.core.source.DataSource;
import io.xpipe.core.source.DataSourceReference;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;

public class CoreJacksonModule extends SimpleModule {

    @Override
    public void setupModule(SetupContext context) {
        context.registerSubtypes(
                new NamedType(StdinDataStore.class),
                new NamedType(StdoutDataStore.class),
                new NamedType(LocalDirectoryDataStore.class),
                new NamedType(CollectionEntryDataStore.class),
                new NamedType(LocalStore.class),
                new NamedType(NamedStore.class),
                new NamedType(ValueType.class),
                new NamedType(TupleType.class),
                new NamedType(ArrayType.class),
                new NamedType(WildcardType.class),
                new NamedType(BaseQueryElement.class),
                new NamedType(ChoiceElement.class),
                new NamedType(BusyElement.class),
                new NamedType(HeaderElement.class)
        );

        for (ShellType t : ShellTypes.getAllShellTypes()) {
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

        addSerializer(SecretValue.class, new SecretSerializer());
        addDeserializer(SecretValue.class, new SecretDeserializer());

        addSerializer(DataSourceReference.class, new DataSourceReferenceSerializer());
        addDeserializer(DataSourceReference.class, new DataSourceReferenceDeserializer());

        context.setMixInAnnotations(Throwable.class, ThrowableTypeMixIn.class);
        context.setMixInAnnotations(DataSourceReference.class, DataSourceReferenceTypeMixIn.class);

        context.addSerializers(_serializers);
        context.addDeserializers(_deserializers);

        /*
        TODO: Find better way to supply a default serializer for data sources
         */
        try {
            Class.forName("io.xpipe.extension.ExtensionException");
        } catch (ClassNotFoundException e) {
            addSerializer(DataSource.class, new NullSerializer());
            addDeserializer(DataSource.class, new NullDeserializer());
        }
    }

    public static class NullDeserializer extends JsonDeserializer<DataSource> {

        @Override
        public DataSource deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            return null;
        }
    }

    public static class DataSourceReferenceSerializer extends JsonSerializer<DataSourceReference> {

        @Override
        public void serialize(DataSourceReference value, JsonGenerator jgen, SerializerProvider provider)
                throws IOException {
            jgen.writeString(value.toRefString());
        }
    }

    public static class DataSourceReferenceDeserializer extends JsonDeserializer<DataSourceReference> {

        @Override
        public DataSourceReference deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            return DataSourceReference.parse(p.getValueAsString());
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

    public static class SecretSerializer extends JsonSerializer<SecretValue> {

        @Override
        public void serialize(SecretValue value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            jgen.writeString(value.getEncryptedValue());
        }
    }

    public static class SecretDeserializer extends JsonDeserializer<SecretValue> {

        @Override
        public SecretValue deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            return new SecretValue(p.getValueAsString());
        }
    }

    @JsonSerialize(as = Throwable.class)
    public abstract static class ThrowableTypeMixIn {

        @JsonIdentityInfo(
                generator = ObjectIdGenerators.StringIdGenerator.class,
                property = "$id"
        )
        private Throwable cause;
    }

    @JsonSerialize(as = DataSourceReference.class)
    public abstract static class DataSourceReferenceTypeMixIn {
    }

    public class NullSerializer extends JsonSerializer<Object> {
        public void serialize(Object value, JsonGenerator jgen, SerializerProvider provider)
                throws IOException, JsonProcessingException {
            jgen.writeNull();
        }
    }
}
