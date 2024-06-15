package io.xpipe.core.util;

import java.util.ServiceLoader;
import java.util.function.Consumer;

public interface ModuleLayerLoader {

    static void loadAll(ModuleLayer layer, Consumer<Throwable> errorHandler) {
        var loaded = layer != null
                ? ServiceLoader.load(layer, ModuleLayerLoader.class)
                : ServiceLoader.load(ModuleLayerLoader.class);
        loaded.stream().forEach(moduleLayerLoaderProvider -> {
            var instance = moduleLayerLoaderProvider.get();
            try {
                instance.init(layer);
            } catch (Throwable t) {
                errorHandler.accept(t);
            }
        });
    }

    default void init(ModuleLayer layer) {}

    default void reset() {}
}
