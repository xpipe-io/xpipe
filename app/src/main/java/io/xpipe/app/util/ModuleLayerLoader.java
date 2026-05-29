package io.xpipe.app.util;

import io.xpipe.app.core.AppProperties;

import java.util.ServiceLoader;
import java.util.function.Consumer;

public interface ModuleLayerLoader {

    static void loadAll(ModuleLayer layer, Consumer<Throwable> errorHandler) {
        var loaded = ServiceLoader.load(layer, ModuleLayerLoader.class);
        loaded.stream().map(ServiceLoader.Provider::get)
                .filter(p -> p.initForCli() || !AppProperties.get().isCli()).forEach(p -> {
            try {
                p.init(layer);
            } catch (Throwable t) {
                errorHandler.accept(t);
            }
        });
    }

    default void init(ModuleLayer layer) {}

    boolean initForCli();
}
