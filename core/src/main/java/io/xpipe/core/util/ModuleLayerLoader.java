package io.xpipe.core.util;

import java.util.ServiceLoader;
import java.util.function.Consumer;

public interface ModuleLayerLoader {

    static void loadAll(
            ModuleLayer layer, boolean hasDaemon, boolean prioritization, Consumer<Throwable> errorHandler
    ) {
        ServiceLoader.load(layer, ModuleLayerLoader.class).stream().forEach(moduleLayerLoaderProvider -> {
            var instance = moduleLayerLoaderProvider.get();
            try {
                if (instance.requiresFullDaemon() && !hasDaemon) {
                    return;
                }

                if (instance.prioritizeLoading() != prioritization) {
                    return;
                }

                instance.init(layer);
            } catch (Throwable t) {
                errorHandler.accept(t);
            }
        });
    }

    void init(ModuleLayer layer);

    boolean requiresFullDaemon();

    boolean prioritizeLoading();
}
