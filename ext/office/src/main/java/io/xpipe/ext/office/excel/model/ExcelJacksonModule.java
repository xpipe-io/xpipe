package io.xpipe.ext.office.excel.model;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

public class ExcelJacksonModule extends SimpleModule {

    @Override
    public void setupModule(SetupContext context) {
        addSerializer(ExcelCellLocation.class, new CellSerializer());
        addDeserializer(ExcelCellLocation.class, new CellDeserializer());

        addSerializer(ExcelRange.class, new RangeSerializer());
        addDeserializer(ExcelRange.class, new RangeDeserializer());

        context.addSerializers(_serializers);
        context.addDeserializers(_deserializers);
    }

    public static class CellSerializer extends StdSerializer<ExcelCellLocation> {

        public CellSerializer() {
            super(ExcelCellLocation.class);
        }

        @Override
        public void serialize(ExcelCellLocation value, JsonGenerator gen, SerializerProvider provider)
                throws IOException {
            gen.writeString(value.toString());
        }
    }

    public static class CellDeserializer extends StdDeserializer<ExcelCellLocation> {

        public CellDeserializer() {
            super(ExcelCellLocation.class);
        }

        @Override
        public ExcelCellLocation deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
            JsonNode node = jp.getCodec().readTree(jp);
            return ExcelCellLocation.parse(node.textValue());
        }
    }

    public static class RangeSerializer extends StdSerializer<ExcelRange> {

        public RangeSerializer() {
            super(ExcelRange.class);
        }

        @Override
        public void serialize(ExcelRange value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeString(value.toString());
        }
    }

    public static class RangeDeserializer extends StdDeserializer<ExcelRange> {

        public RangeDeserializer() {
            super(ExcelRange.class);
        }

        @Override
        public ExcelRange deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
            JsonNode node = jp.getCodec().readTree(jp);
            return ExcelRange.parse(node.asText());
        }
    }
}
