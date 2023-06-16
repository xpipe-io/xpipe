package io.xpipe.app.ext;

import com.fasterxml.jackson.databind.jsontype.NamedType;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.util.XPipeDaemon;
import io.xpipe.core.process.ProcessControlProvider;
import io.xpipe.core.util.JacksonMapper;
import io.xpipe.core.util.ModuleLayerLoader;
import io.xpipe.core.util.ProxyFunction;

public class XPipeServiceProviders {

    public static void load(ModuleLayer layer) {
        var hasDaemon = XPipeDaemon.getInstanceIfPresent().isPresent();
        ModuleLayerLoader.loadAll(layer, hasDaemon, true, t -> {
            ErrorEvent.fromThrowable(t).handle();
        });
        ProcessControlProvider.init(layer);

        TrackEvent.info("Loading extension providers ...");
        DataSourceProviders.init(layer);
        for (DataSourceProvider<?> p : DataSourceProviders.getAll()) {
            TrackEvent.trace("Loaded data source provider " + p.getId());
            JacksonMapper.configure(objectMapper -> {
                objectMapper.registerSubtypes(new NamedType(p.getSourceClass()));
            });
        }

        DataStoreProviders.init(layer);
        for (DataStoreProvider p : DataStoreProviders.getAll()) {
            TrackEvent.trace("Loaded data store provider " + p.getId());
            JacksonMapper.configure(objectMapper -> {
                for (Class<?> storeClass : p.getStoreClasses()) {
                    objectMapper.registerSubtypes(new NamedType(storeClass));
                }
            });
        }

        ModuleLayerLoader.loadAll(layer, hasDaemon, false, t -> {
            ErrorEvent.fromThrowable(t).handle();
        });

        if (hasDaemon) {
            ProxyFunction.init(layer);
        }

        TrackEvent.info("Finished loading extension providers");
    }
}
