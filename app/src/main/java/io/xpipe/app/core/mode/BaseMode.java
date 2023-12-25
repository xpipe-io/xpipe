package io.xpipe.app.core.mode;

import io.xpipe.app.browser.BrowserModel;
import io.xpipe.app.comp.store.StoreViewState;
import io.xpipe.app.core.*;
import io.xpipe.app.core.check.AppAvBlockCheck;
import io.xpipe.app.core.check.AppMalwarebytesCheck;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.update.XPipeDistributionType;
import io.xpipe.app.util.FileBridge;
import io.xpipe.app.util.LicenseProvider;
import io.xpipe.app.util.LocalShell;
import io.xpipe.app.util.LockedSecretValue;
import io.xpipe.core.util.JacksonMapper;

public class BaseMode extends OperationMode {

    private boolean initialized;

    @Override
    public boolean isSupported() {
        return true;
    }

    @Override
    public String getId() {
        return "background";
    }

    @Override
    public void onSwitchTo() throws Throwable {
        if (initialized) {
            return;
        }

        // For debugging
        // if (true) throw new IllegalStateException();

        TrackEvent.info("mode", "Initializing base mode components ...");
        AppExtensionManager.init(true);
        JacksonMapper.initModularized(AppExtensionManager.getInstance().getExtendedLayer());
        JacksonMapper.configure(objectMapper -> {
            objectMapper.registerSubtypes(LockedSecretValue.class);
        });
        // Load translations before storage initialization to localize store error messages
        // Also loaded before antivirus alert to localize that
        AppI18n.init();
        LicenseProvider.get().init();
        AppAvBlockCheck.check();
        AppMalwarebytesCheck.check();
        LocalShell.init();
        XPipeDistributionType.init();
        AppPrefs.init();
        AppCharsets.init();
        AppCharsetter.init();
        AppSocketServer.init();
        DataStorage.init();
        AppFileWatcher.init();
        FileBridge.init();
        ActionProvider.initProviders();
        TrackEvent.info("mode", "Finished base components initialization");
        initialized = true;
    }

    @Override
    public void onSwitchFrom() {}

    @Override
    public void finalTeardown() {
        TrackEvent.info("mode", "Background mode shutdown started");
        BrowserModel.DEFAULT.reset();
        StoreViewState.reset();
        DataStorage.reset();
        AppPrefs.reset();
        AppResources.reset();
        AppExtensionManager.reset();
        AppDataLock.unlock();
        // Shut down socket server last to keep a non-daemon thread running
        AppSocketServer.reset();
        TrackEvent.info("mode", "Background mode shutdown finished");
    }
}
