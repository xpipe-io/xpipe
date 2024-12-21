package io.xpipe.app.core.mode;

import io.xpipe.app.beacon.AppBeaconServer;
import io.xpipe.app.beacon.BlobManager;
import io.xpipe.app.browser.BrowserFullSessionModel;
import io.xpipe.app.browser.file.BrowserLocalFileSystem;
import io.xpipe.app.browser.icon.BrowserIconManager;
import io.xpipe.app.comp.base.AppLayoutComp;
import io.xpipe.app.comp.store.StoreViewState;
import io.xpipe.app.core.*;
import io.xpipe.app.core.check.*;
import io.xpipe.app.core.window.AppMainWindow;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.ext.DataStoreProviders;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.resources.AppImages;
import io.xpipe.app.resources.AppResources;
import io.xpipe.app.resources.SystemIcons;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStorageSyncHandler;
import io.xpipe.app.terminal.TerminalLauncherManager;
import io.xpipe.app.terminal.TerminalView;
import io.xpipe.app.update.UpdateAvailableAlert;
import io.xpipe.app.update.UpdateChangelogAlert;
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
        LicenseProvider.get().init();
        AppCertutilCheck.check();
        AppBundledToolsCheck.check();
        AppHomebrewCoreutilsCheck.check();
        AppAvCheck.check();
        AppJavaOptionsCheck.check();
        AppSid.init();
        ThreadHelper.loadParallel(true, () -> {
            LocalShell.init();
            AppShellCheck.check();
            AppRosettaCheck.check();
            AppTestCommandCheck.check();
            XPipeDistributionType.init();
            AppPrefs.setLocalDefaultsIfNeeded();
        }, () -> {
            // Initialize beacon server as we should be prepared for git askpass commands
            AppBeaconServer.init();
            DataStorageSyncHandler.getInstance().init();
            DataStorageSyncHandler.getInstance().retrieveSyncedData();
            AppPrefs.initSharedRemote();
            DataStorage.init();
            StoreViewState.init();
            AppLayoutModel.init();
            PlatformInit.init(true);
            PlatformThread.runLaterIfNeededBlocking(() -> {
                AppGreetings.showIfNeeded();
                AppMainWindow.initContent();
            });
        }, () -> {
            AppFileWatcher.init();
            FileBridge.init();
            BlobManager.init();
            TerminalView.init();
            TerminalLauncherManager.init();
        }, () -> {
            PlatformInit.init(true);
            AppImages.init();
            SystemIcons.init();
        }, () -> {
            // If we downloaded an update, and decided to no longer automatically update, don't remind us!
            // You can still update manually in the about tab
            if (AppPrefs.get().automaticallyUpdate().get()
                    || AppPrefs.get().checkForSecurityUpdates().get()) {
                UpdateAvailableAlert.showIfNeeded();
            }
            UpdateChangelogAlert.showIfNeeded();
        }, () -> {
            BrowserIconManager.loadIfNecessary();
        }, () -> {
            BrowserLocalFileSystem.init();
        });
        ActionProvider.initProviders();
        DataStoreProviders.init();

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
