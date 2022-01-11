package io.xpipe.core.util;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

public class JacksonHelper {

    private static final ObjectMapper INSTANCE = new ObjectMapper();
    private static boolean init = false;

    public static synchronized void init(ModuleLayer layer) {
        ObjectMapper objectMapper = INSTANCE;
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        objectMapper.registerModules(findModules(layer));
        objectMapper.setVisibility(objectMapper.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE)
                .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE));
        init = true;
    }

    private static List<Module> findModules(ModuleLayer layer) {
        ArrayList<Module> modules = new ArrayList<Module>();
        ServiceLoader<Module> loader = ServiceLoader.load(layer, Module.class);
        for (Module module : loader) {
            modules.add(module);
        }
        return modules;
    }

    /**
     * Constructs a new ObjectMapper that is able to map all required X-Pipe classes and also possible extensions.
     */
    public static ObjectMapper newMapper() {
        if (!init) {
            throw new IllegalStateException("Not initialized");
        }

        return INSTANCE.copy();
    }

    public static boolean isInit() {
        return init;
    }
}
