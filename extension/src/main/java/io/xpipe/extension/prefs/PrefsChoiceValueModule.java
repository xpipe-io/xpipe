package io.xpipe.extension.prefs;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;
import java.util.List;

public class PrefsChoiceValueModule extends SimpleModule {

    @Override
    public void setupModule(SetupContext context) {
        addSerializer(PrefsChoiceValue.class, new PrefsChoiceValueSerializer());
        addDeserializer(PrefsChoiceValue.class, new PrefsChoiceValueDeserializer());

        context.addSerializers(_serializers);
        context.addDeserializers(_deserializers);
    }

    public static class PrefsChoiceValueSerializer extends JsonSerializer<PrefsChoiceValue> {

        @Override
        public void serialize(PrefsChoiceValue value, JsonGenerator jgen, SerializerProvider provider)
                throws IOException {
            jgen.writeString(value.getId());
        }
    }

    public static class PrefsChoiceValueDeserializer extends JsonDeserializer<PrefsChoiceValue> {

        @Override
        public PrefsChoiceValue deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            var id = p.getValueAsString();
            Class<? extends PrefsChoiceValue> clazz = (Class<? extends PrefsChoiceValue>) ctxt.getContextualType().getRawClass();
            try {
                var list = (List<? extends PrefsChoiceValue>) clazz.getDeclaredField("SUPPORTED").get(null);
                return list.stream().filter(v -> v.getId().equals(id)).findAny().orElse(null);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
