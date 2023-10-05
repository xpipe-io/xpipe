package io.xpipe.app.util;

import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.storage.GitStorageHandler;
import io.xpipe.core.util.ModuleLayerLoader;

import java.util.ServiceLoader;

public abstract class FeatureProvider {

    private static FeatureProvider INSTANCE = null;

    public static FeatureProvider get() {
        return INSTANCE;
    }

    public static class Loader implements ModuleLayerLoader {

        @Override
        public void init(ModuleLayer layer) {
            INSTANCE = ServiceLoader.load(layer, FeatureProvider.class).stream()
                               .map(ServiceLoader.Provider::get)
                               .findFirst().orElseThrow();
        }

        @Override
        public boolean requiresFullDaemon() {
            return true;
        }

        @Override
        public boolean prioritizeLoading() {
            return true;
        }
    }

    public abstract LicenseType getLicenseType();

    public abstract void init();

    public abstract Comp<?> overviewPage();

    public abstract GitStorageHandler createStorageHandler();
}
