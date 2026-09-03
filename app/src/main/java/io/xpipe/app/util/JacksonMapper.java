package io.xpipe.app.util;

import io.xpipe.app.ext.ModuleLayerLoader;
import io.xpipe.app.store.DataStoreProvider;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.core.util.DefaultIndenter;
import tools.jackson.core.util.DefaultPrettyPrinter;
import tools.jackson.databind.*;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.module.SimpleModule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ServiceLoader;

public class JacksonMapper {

    private static final JsonMapper BASE = create();
    private static final List<JacksonModule> MODULES = new ArrayList<>();
    private static JsonMapper INSTANCE = BASE;

    private static JsonMapper create() {
        JsonMapper.Builder builder = JsonMapper.builder();

        // Reflection config
        builder.changeDefaultVisibility(visibilityChecker -> {
            return visibilityChecker
                    .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                    .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                    .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                    .withCreatorVisibility(JsonAutoDetect.Visibility.NONE)
                    .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE);
        });

        // Write format config
        builder.enable(SerializationFeature.INDENT_OUTPUT)
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .defaultPrettyPrinter(
                        new DefaultPrettyPrinter().withObjectIndenter(new DefaultIndenter().withLinefeed("\n")));

        // Read config
        builder.enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION)
                .disable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE)
                .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
                .enable(JsonReadFeature.ALLOW_YAML_COMMENTS)
                .enable(JsonReadFeature.ALLOW_TRAILING_COMMA)
                .enable(EnumFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_VALUES);

        return builder.build();
    }

    private static List<JacksonModule> findModules(ModuleLayer layer) {
        ArrayList<JacksonModule> modules = new ArrayList<>();
        ServiceLoader<JacksonModule> loader = ServiceLoader.load(layer, JacksonModule.class);
        for (JacksonModule module : loader) {
            modules.add(module);
        }
        return modules;
    }

    public static JsonMapper getWithoutModules(Class<?>... classes) {
        var mods = new ArrayList<>(MODULES);
        mods.removeIf(
                jacksonModule -> Arrays.stream(classes).anyMatch(aClass -> aClass.equals(jacksonModule.getClass())));
        return BASE.rebuild().addModules(mods).build();
    }

    public static JsonMapper getDefault() {
        return INSTANCE;
    }

    public static JsonMapper getRedactedSecretMapper() {
        var b = INSTANCE.rebuild();
        b.disable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION);
        b.addModule(new SimpleModule() {
            @Override
            public void setupModule(SetupContext context) {
                addSerializer(SecretValue.class, new ValueSerializer<>() {
                    @Override
                    public void serialize(SecretValue value, JsonGenerator gen, SerializationContext context) {
                        gen.writeString("<secret>");
                    }

                    @Override
                    public void serializeWithType(
                            SecretValue value,
                            JsonGenerator gen,
                            SerializationContext context,
                            TypeSerializer typeSer) {
                        gen.writeString("<secret>");
                    }
                });
                super.setupModule(context);
            }
        });
        return b.build();
    }

    public static JsonMapper getUnredactSecretMapper() {
        var b = INSTANCE.rebuild();
        b.addModule(new SimpleModule() {
            @Override
            public void setupModule(SetupContext context) {
                addSerializer(SecretValue.class, new ValueSerializer<>() {
                    @Override
                    public void serialize(SecretValue value, JsonGenerator gen, SerializationContext context) {
                        gen.writeString(value.getSecretValue());
                    }

                    @Override
                    public void serializeWithType(
                            SecretValue value,
                            JsonGenerator gen,
                            SerializationContext context,
                            TypeSerializer typeSer) {
                        gen.writeString(value.getSecretValue());
                    }
                });
                super.setupModule(context);
            }
        });
        return b.build();
    }

    public static class Loader implements ModuleLayerLoader {

        @Override
        public void init(ModuleLayer layer) {
            MODULES.addAll(findModules(layer));

            var b = INSTANCE.rebuild().addModules(MODULES);
            if (DataStoreProvider.getAll() != null) {
                var providerModule = new SimpleModule() {

                    @Override
                    public void setupModule(SetupContext context) {
                        for (DataStoreProvider p : DataStoreProvider.getAll()) {
                            registerSubtypes(p.getStoreClasses());
                        }

                        super.setupModule(context);
                    }
                };
                b.addModules(providerModule);
            }

            INSTANCE = b.build();
        }

        @Override
        public boolean initForCli() {
            return true;
        }
    }
}
