package io.xpipe.extension.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.xpipe.core.source.DataSource;
import io.xpipe.core.util.JacksonHelper;
import io.xpipe.extension.DataSourceProviders;

import java.io.IOException;

public class ExtensionJacksonModule extends SimpleModule {

    @Override
    public void setupModule(SetupContext context) {
        addSerializer(DataSource.class, new DataSourceSerializer());
        addDeserializer(DataSource.class, new DataSourceDeserializer());

        context.addSerializers(_serializers);
        context.addDeserializers(_deserializers);
    }

    public static class DataSourceSerializer extends JsonSerializer<DataSource> {

        @Override
        public void serialize(DataSource value, JsonGenerator jgen, SerializerProvider provider)
                throws IOException {

            ObjectMapper mapper = JacksonHelper.newMapper(ExtensionJacksonModule.class);
            var prov = DataSourceProviders.byDataSourceClass(value.getClass());
            ObjectNode objectNode = mapper.valueToTree(value);
            objectNode.put("type", prov.getId());
            jgen.writeTree(objectNode);
        }
    }

    public static class DataSourceDeserializer extends JsonDeserializer<DataSource> {

        @Override
        public DataSource deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            var mapper = JacksonHelper.newMapper(ExtensionJacksonModule.class);
            var tree = (ObjectNode) mapper.readTree(p);
            var type = tree.get("type").textValue();
            var prov = DataSourceProviders.byName(type);
            if (prov.isEmpty()) {
                return null;
            }

            return mapper.treeToValue(tree, prov.get().getSourceClass());
        }
    }
}
