package io.xpipe.core.util;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import lombok.Getter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.Consumer;

public class JacksonMapper {

    private static final ObjectMapper BASE = new ObjectMapper();
    private static final ObjectMapper INSTANCE;

    @Getter
    private static boolean init = false;

    static {
        configureBase(BASE);
        INSTANCE = BASE.copy();
    }

    @SuppressWarnings("deprecation")
    private static void configureBase(ObjectMapper objectMapper) {
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.enable(JsonParser.Feature.ALLOW_COMMENTS);
        objectMapper.enable(JsonParser.Feature.ALLOW_TRAILING_COMMA);
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.disable(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE);
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.setVisibility(objectMapper
                .getSerializationConfig()
                .getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE)
                .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE));
    }

    public static <T> T parse(String s, Class<T> c) throws JsonProcessingException {
        var mapper = getDefault();
        var tree = mapper.readTree(s);
        return mapper.treeToValue(tree, c);
    }

    public static synchronized void configure(Consumer<ObjectMapper> mapper) {
        mapper.accept(INSTANCE);
    }

    public static class Loader implements ModuleLayerLoader {

        @Override
        public void init(ModuleLayer layer) {
            List<Module> modules = findModules(layer);
            INSTANCE.registerModules(modules);
            var extensions = findExtensions(layer);
            for (var extension : extensions) {
                var mod = new SimpleModule();
                if (extension instanceof JsonSerializer<?> s) {
                    add(mod, extension.getType(), s);
                }
                if (extension instanceof JsonDeserializer<?> d) {
                    add(mod, extension.getType(), d);
                }
                INSTANCE.registerModule(mod);
            }
            init = true;
        }

        @SuppressWarnings("unchecked")
        private <T> void add(SimpleModule mod, Class<?> c, JsonSerializer<?> s) {
            mod.addSerializer((Class<T>) c, (JsonSerializer<T>) s);
        }

        @SuppressWarnings("unchecked")
        private <T> void add(SimpleModule mod, Class<?> c, JsonDeserializer<?> s) {
            mod.addDeserializer((Class<T>) c, (JsonDeserializer<T>) s);
        }
    }

    private static List<Module> findModules(ModuleLayer layer) {
        ArrayList<Module> modules = new ArrayList<>();
        ServiceLoader<Module> loader =
                layer != null ? ServiceLoader.load(layer, Module.class) : ServiceLoader.load(Module.class);
        for (Module module : loader) {
            modules.add(module);
        }
        return modules;
    }

    private static List<JacksonExtension> findExtensions(ModuleLayer layer) {
        ArrayList<JacksonExtension> exts = new ArrayList<>();
        ServiceLoader<JacksonExtension> loader = layer != null
                ? ServiceLoader.load(layer, JacksonExtension.class)
                : ServiceLoader.load(JacksonExtension.class);
        for (JacksonExtension module : loader) {
            exts.add(module);
        }
        return exts;
    }

    /**
     * Constructs a new ObjectMapper that is able to map all required XPipe classes and also possible extensions.
     */
    public static ObjectMapper newMapper() {
        if (!JacksonMapper.isInit()) {
            return BASE;
        }

        return INSTANCE.copy();
    }

    public static ObjectMapper getDefault() {
        if (!JacksonMapper.isInit()) {
            return BASE;
        }

        return INSTANCE;
    }

    public static ObjectMapper getCensored() {
        if (!JacksonMapper.isInit()) {
            return BASE;
        }

        var c = INSTANCE.copy();
        c.registerModule(new SimpleModule() {
            @Override
            public void setupModule(SetupContext context) {
                addSerializer(SecretValue.class, new JsonSerializer<>() {
                    @Override
                    public void serialize(SecretValue value, JsonGenerator gen, SerializerProvider serializers)
                            throws IOException {
                        gen.writeString("<secret>");
                    }

                    @Override
                    public void serializeWithType(
                            SecretValue value,
                            JsonGenerator gen,
                            SerializerProvider serializers,
                            TypeSerializer typeSer)
                            throws IOException {
                        gen.writeString("<secret>");
                    }
                });
                super.setupModule(context);
            }
        });
        return c;
    }
}
