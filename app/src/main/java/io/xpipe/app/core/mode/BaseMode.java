package io.xpipe.app.core.mode;

import io.xpipe.app.beacon.AppBeaconServer;
import io.xpipe.app.beacon.BlobManager;
import io.xpipe.app.browser.BrowserFullSessionModel;
import io.xpipe.app.comp.store.StoreViewState;
import io.xpipe.app.core.*;
import io.xpipe.app.core.check.*;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.ext.DataStoreProviders;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.resources.AppResources;
import io.xpipe.app.resources.SystemIcons;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStorageSyncHandler;
import io.xpipe.app.terminal.TerminalLauncherManager;
import io.xpipe.app.terminal.TerminalView;
import io.xpipe.app.update.XPipeDistributionType;
import io.xpipe.app.util.*;

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
        AppI18n.init();
        LicenseProvider.get().init();
        AppCertutilCheck.check();
        AppBundledToolsCheck.check();
        AppHomebrewCoreutilsCheck.check();
        AppAvCheck.check();
        AppJavaOptionsCheck.check();
        AppSid.init();
        LocalShell.init();
        AppShellCheck.check();
        AppRosettaCheck.check();
        AppTestCommandCheck.check();
        XPipeDistributionType.init();
        AppPrefs.setLocalDefaultsIfNeeded();
        // Initialize beacon server as we should be prepared for git askpass commands
        AppBeaconServer.init();
        DataStorageSyncHandler.getInstance().init();
        DataStorageSyncHandler.getInstance().retrieveSyncedData();
        AppPrefs.initSharedRemote();
        UnlockAlert.showIfNeeded();
        SystemIcons.init();
        DataStorage.init();
        DataStoreProviders.init();
        AppFileWatcher.init();
        FileBridge.init();
        BlobManager.init();
        ActionProvider.initProviders();
        TerminalView.init();
        TerminalLauncherManager.init();
        TrackEvent.info("Finished base components initialization");
        initialized = true;
    }

    @Override
    public void onSwitchFrom() {}

    @Override
    public void finalTeardown() throws Exception {
        TrackEvent.info("Background mode shutdown started");
        BrowserFullSessionModel.DEFAULT.reset();
        SshLocalBridge.reset();
        StoreViewState.reset();
        DataStoreProviders.reset();
        DataStorage.reset();
        AppPrefs.reset();
        DataStorageSyncHandler.getInstance().reset();
        LocalShell.reset();
        ProcessControlProvider.get().reset();
        AppResources.reset();
        AppExtensionManager.reset();
        AppDataLock.unlock();
        BlobManager.reset();
        FileBridge.reset();
        AppBeaconServer.reset();
        TrackEvent.info("Background mode shutdown finished");
    }
}
