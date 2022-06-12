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
import io.xpipe.core.config.BaseQueryElement;
import io.xpipe.core.config.ChoiceElement;
import io.xpipe.core.config.HeaderElement;
import io.xpipe.core.data.type.ArrayType;
import io.xpipe.core.data.type.TupleType;
import io.xpipe.core.data.type.ValueType;
import io.xpipe.core.data.type.WildcardType;
import io.xpipe.core.source.DataSourceInfo;
import io.xpipe.core.source.DataSourceReference;
import io.xpipe.core.store.CollectionEntryDataStore;
import io.xpipe.core.store.HttpRequestStore;
import io.xpipe.core.store.LocalDirectoryDataStore;
import io.xpipe.core.store.LocalFileDataStore;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;

public class CoreJacksonModule extends SimpleModule {

    @Override
    public void setupModule(SetupContext context) {
        context.registerSubtypes(
                new NamedType(LocalFileDataStore.class),
                new NamedType(LocalDirectoryDataStore.class),
                new NamedType(CollectionEntryDataStore.class),
                new NamedType(HttpRequestStore.class),
                new NamedType(ValueType.class),
                new NamedType(TupleType.class),
                new NamedType(ArrayType.class),
                new NamedType(WildcardType.class),
                new NamedType(DataSourceInfo.Table.class),
                new NamedType(DataSourceInfo.Structure.class),
                new NamedType(DataSourceInfo.Text.class),
                new NamedType(DataSourceInfo.Collection.class),
                new NamedType(DataSourceInfo.Raw.class),
                new NamedType(BaseQueryElement.class),
                new NamedType(ChoiceElement.class),
                new NamedType(HeaderElement.class)
        );

        addSerializer(Charset.class, new CharsetSerializer());
        addDeserializer(Charset.class, new CharsetDeserializer());

        addSerializer(Path.class, new LocalPathSerializer());
        addDeserializer(Path.class, new LocalPathDeserializer());

        addSerializer(DataSourceReference.class, new DataSourceReferenceSerializer());
        addDeserializer(DataSourceReference.class, new DataSourceReferenceDeserializer());

        context.setMixInAnnotations(Throwable.class, ThrowableTypeMixIn.class);
        context.setMixInAnnotations(DataSourceReference.class, DataSourceReferenceTypeMixIn.class);

        context.addSerializers(_serializers);
        context.addDeserializers(_deserializers);
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
        public void serialize(Charset value, JsonGenerator jgen, SerializerProvider provider)
                throws IOException {
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
        public void serialize(Path value, JsonGenerator jgen, SerializerProvider provider)
                throws IOException {
            jgen.writeString(value.toString());
        }
    }

    public static class LocalPathDeserializer extends JsonDeserializer<Path> {

        @Override
        public Path deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            return Path.of(p.getValueAsString());
        }
    }

    @JsonSerialize(as = Throwable.class)
    public abstract static class ThrowableTypeMixIn {

        @JsonIdentityInfo(generator= ObjectIdGenerators.StringIdGenerator.class, property="$id")
        private Throwable cause;
    }

    @JsonSerialize(as = DataSourceReference.class)
    public abstract static class DataSourceReferenceTypeMixIn {
    }
}
