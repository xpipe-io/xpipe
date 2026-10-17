package io.xpipe.app.ext;

import io.xpipe.app.core.AppProperties;

import java.util.ServiceLoader;
import java.util.function.Consumer;

public interface ModuleLayerLoader {

    static void loadAll(ModuleLayer layer, Consumer<Throwable> errorHandler) {
        var loaded = ServiceLoader.load(layer, ModuleLayerLoader.class);
        loaded.stream().forEach(provider -> {
            try {
                var p = provider.get();
                if (!p.initForCli() && AppProperties.get().isCli()) {
                    return;
                }
                p.init(layer);
            } catch (Throwable t) {
                errorHandler.accept(t);
            }
        });
    }

    default void init(ModuleLayer layer) {}

    boolean initForCli();
}
