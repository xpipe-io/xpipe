package io.xpipe.app.core.mode;

import io.xpipe.app.browser.BrowserModel;
import io.xpipe.app.comp.store.StoreViewState;
import io.xpipe.app.core.*;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.util.FileBridge;
import io.xpipe.app.util.LicenseProvider;
import io.xpipe.app.util.LockedSecretValue;
import io.xpipe.core.store.LocalStore;
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
        AppAntivirusAlert.showIfNeeded();
        LocalStore.init();
        AppPrefs.init();
        AppCharsets.init();
        AppCharsetter.init();
        DataStorage.init();
        AppFileWatcher.init();
        FileBridge.init();
        AppSocketServer.init();
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
        AppExtensionManager.reset();
        AppDataLock.unlock();
        // Shut down socket server last to keep a non-daemon thread running
        AppSocketServer.reset();
        TrackEvent.info("mode", "Background mode shutdown finished");
    }
}
