package io.xpipe.core;

import java.util.ServiceLoader;
import java.util.function.Consumer;

public interface ModuleLayerLoader {

    static void loadAll(ModuleLayer layer, Consumer<Throwable> errorHandler) {
        var loaded = ServiceLoader.load(layer, ModuleLayerLoader.class);
        loaded.stream().map(ServiceLoader.Provider::get).filter(p -> p.initForCli() || !AppPro).forEach(moduleLayerLoaderProvider -> {
            var instance = moduleLayerLoaderProvider.get();
            try {
                instance.init(layer);
            } catch (Throwable t) {
                errorHandler.accept(t);
            }
        });
    }

    default void init(ModuleLayer layer) {}

    boolean initForCli();
}
