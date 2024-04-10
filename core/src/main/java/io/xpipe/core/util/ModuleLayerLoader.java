package io.xpipe.core.util;

import java.util.ServiceLoader;
import java.util.function.Consumer;

public interface ModuleLayerLoader {

    static void loadAll(ModuleLayer layer, Consumer<Throwable> errorHandler) {
        ServiceLoader.load(layer, ModuleLayerLoader.class).stream().forEach(moduleLayerLoaderProvider -> {
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
