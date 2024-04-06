package io.xpipe.app.core.mode;

import io.xpipe.app.browser.session.BrowserSessionModel;
import io.xpipe.app.comp.store.StoreViewState;
import io.xpipe.app.core.*;
import io.xpipe.app.core.check.AppAvCheck;
import io.xpipe.app.core.check.AppCertutilCheck;
import io.xpipe.app.core.check.AppShellCheck;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.GitStorageHandler;
import io.xpipe.app.update.XPipeDistributionType;
import io.xpipe.app.util.FileBridge;
import io.xpipe.app.util.LicenseProvider;
import io.xpipe.app.util.LocalShell;
import io.xpipe.app.util.UnlockAlert;
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

        TrackEvent.info("Initializing base mode components ...");
        AppExtensionManager.init(true);
        JacksonMapper.initModularized(AppExtensionManager.getInstance().getExtendedLayer());
        AppI18n.init();
        LicenseProvider.get().init();
        AppPrefs.initLocal();
        AppI18n.init();
        AppCertutilCheck.check();
        AppAvCheck.check();
        AppSid.init();
        LocalShell.init();
        AppShellCheck.check();
        XPipeDistributionType.init();
        AppPrefs.setDefaults();
        // Initialize socket server as we should be prepared for git askpass commands
        AppSocketServer.init();
        GitStorageHandler.getInstance().init();
        GitStorageHandler.getInstance().setupRepositoryAndPull();
        AppPrefs.initSharedRemote();
        UnlockAlert.showIfNeeded();
        DataStorage.init();
        AppFileWatcher.init();
        FileBridge.init();
        ActionProvider.initProviders();
        TrackEvent.info("Finished base components initialization");
        initialized = true;
    }

    @Override
    public void onSwitchFrom() {}

    @Override
    public void finalTeardown() {
        TrackEvent.info("Background mode shutdown started");
        BrowserSessionModel.DEFAULT.reset();
        StoreViewState.reset();
        DataStorage.reset();
        AppPrefs.reset();
        AppResources.reset();
        AppExtensionManager.reset();
        AppDataLock.unlock();
        // Shut down socket server last to keep a non-daemon thread running
        AppSocketServer.reset();
        TrackEvent.info("Background mode shutdown finished");
    }
}
