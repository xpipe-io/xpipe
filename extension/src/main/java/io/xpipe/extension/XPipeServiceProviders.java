package io.xpipe.extension;

import com.fasterxml.jackson.databind.jsontype.NamedType;
import io.xpipe.core.impl.LocalProcessControlProvider;
import io.xpipe.core.util.JacksonMapper;
import io.xpipe.core.util.ProxyFunction;
import io.xpipe.extension.event.TrackEvent;
import io.xpipe.extension.prefs.PrefsProvider;
import io.xpipe.extension.util.ActionProvider;
import io.xpipe.extension.util.ModuleLayerLoader;
import io.xpipe.extension.util.XPipeDaemon;

public class XPipeServiceProviders {

    public static void load(ModuleLayer layer) {
        LocalProcessControlProvider.init(layer);

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

        var hasDaemon = XPipeDaemon.getInstanceIfPresent().isPresent();
        ModuleLayerLoader.loadAll(layer, hasDaemon);

        if (hasDaemon) {
            ActionProvider.init(layer);
            DataSourceActionProvider.init(layer);
            ProxyFunction.init(layer);
            PrefsProvider.init(layer);
        }

        TrackEvent.info("Finished loading extension providers");
    }
}
