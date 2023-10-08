package io.xpipe.app.util;

import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.storage.GitStorageHandler;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.util.ModuleLayerLoader;

import java.util.ServiceLoader;

public abstract class LicenseProvider {

    private static LicenseProvider INSTANCE = null;

    public static LicenseProvider get() {
        return INSTANCE;
    }

    public static class Loader implements ModuleLayerLoader {

        @Override
        public void init(ModuleLayer layer) {
            INSTANCE = ServiceLoader.load(layer, LicenseProvider.class).stream()
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

    public abstract void handleShellControl(ShellControl sc);

    public abstract void showLicenseAlert(LicenseRequiredException ex);

    public abstract LicenseType getLicenseType();

    public abstract void init();

    public abstract Comp<?> overviewPage();

    public abstract GitStorageHandler createStorageHandler();
}
