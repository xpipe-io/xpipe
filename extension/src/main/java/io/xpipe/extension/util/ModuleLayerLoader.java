package io.xpipe.extension.util;

import io.xpipe.extension.event.ErrorEvent;

import java.util.ServiceLoader;

public interface ModuleLayerLoader {

    public static void loadAll(ModuleLayer layer, boolean hasDaemon) {
        ServiceLoader.load(layer, ModuleLayerLoader.class).stream().forEach(moduleLayerLoaderProvider -> {
            var instance = moduleLayerLoaderProvider.get();
            try {
                if (instance.requiresFullDaemon() && !hasDaemon) {
                    return;
                }
                instance.init(layer);
            } catch (Throwable t) {
                ErrorEvent.fromThrowable(t).handle();
            }
        });
    }

    public void init(ModuleLayer layer);

    boolean requiresFullDaemon();
}
