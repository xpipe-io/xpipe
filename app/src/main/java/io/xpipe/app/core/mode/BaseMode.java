package io.xpipe.app.core.mode;

import io.xpipe.app.action.AbstractAction;
import io.xpipe.app.action.ActionProvider;
import io.xpipe.app.beacon.AppBeaconServer;
import io.xpipe.app.beacon.BlobManager;
import io.xpipe.app.browser.BrowserFullSessionModel;
import io.xpipe.app.browser.file.BrowserLocalFileSystem;
import io.xpipe.app.browser.icon.BrowserIconManager;
import io.xpipe.app.core.*;
import io.xpipe.app.core.check.*;
import io.xpipe.app.core.window.AppDialog;
import io.xpipe.app.core.window.AppMainWindow;
import io.xpipe.app.core.window.AppWindowTitle;
import io.xpipe.app.ext.DataStoreProviders;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.hub.comp.StoreViewState;
import io.xpipe.app.icon.SystemIconManager;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.pwman.KeePassXcPasswordManager;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStorageSyncHandler;
import io.xpipe.app.terminal.TerminalLauncherManager;
import io.xpipe.app.terminal.TerminalView;
import io.xpipe.app.update.UpdateAvailableDialog;
import io.xpipe.app.update.UpdateChangelogAlert;
import io.xpipe.app.update.UpdateNagDialog;
import io.xpipe.app.util.*;
import io.xpipe.core.XPipeDaemonMode;

import java.util.concurrent.CountDownLatch;

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
    public void onSwitchTo() {
        if (initialized) {
            return;
        }

        // For debugging
        // if (true) throw new IllegalStateException();

        TrackEvent.info("Initializing base mode components ...");
        AppMainWindow.loadingText("initializingApp");
        LicenseProvider.get().init();
        AppWindowTitle.init();
        AppPathCorruptCheck.check();
        AppHomebrewCoreutilsCheck.check();
        AppAvCheck.check();
        AppJavaOptionsCheck.check();
        AppSid.init();
        AppBeaconServer.init();
        AppLayoutModel.init();

        if (OperationMode.getStartupMode() == XPipeDaemonMode.GUI) {
            AppPtbDialog.showIfNeeded();
        }

        // If we downloaded an update, and decided to no longer automatically update, don't remind us!
        // You can still update manually in the about tab
        if (AppPrefs.get().automaticallyUpdate().get()
                || AppPrefs.get().checkForSecurityUpdates().get()) {
            UpdateAvailableDialog.showIfNeeded(true);
        } else {
            UpdateNagDialog.showAndWaitIfNeeded();
        }

        var imagesLoaded = new CountDownLatch(1);
        var iconsLoaded = new CountDownLatch(1);
        var browserLoaded = new CountDownLatch(1);
        var shellLoaded = new CountDownLatch(1);
        var storageLoaded = new CountDownLatch(1);
        var localPrefsLoaded = new CountDownLatch(1);
        ThreadHelper.load(
                true,
                () -> {
                    LocalShell.init();
                    AppShellCheck.check();
                    shellLoaded.countDown();
                    AppRosettaCheck.check();
                    AppTestCommandCheck.check();
                    // This might be slow on macOS and might take longer than the platform init
                    AppPrefs.setLocalDefaultsIfNeeded();
                    localPrefsLoaded.countDown();
                    PlatformInit.init(true);
                    TrackEvent.info("Shell initialization thread completed");
                },
                () -> {
                    shellLoaded.await();
                    DataStorageSyncHandler.getInstance().init();
                    if (DataStorageSyncHandler.getInstance().supportsSync()) {
                        AppMainWindow.loadingText("loadingGit");
                    }
                    DataStorageSyncHandler.getInstance().retrieveSyncedData();
                    AppMainWindow.loadingText("loadingSettings");
                    AppPrefs.initWithShell();
                    AppMainWindow.loadingText("loadingConnections");
                    DataStorage.init();
                    storageLoaded.countDown();
                    StoreViewState.init();
                    AppMainWindow.loadingText("loadingSettings");
                    TrackEvent.info("Connection storage initialization thread completed");
                },
                () -> {
                    PlatformInit.init(true);
                    imagesLoaded.await();
                    browserLoaded.await();
                    iconsLoaded.await();
                    localPrefsLoaded.await();
                    AppMainWindow.loadingText("loadingUserInterface");
                    AppMainWindow.initContent();
                    TrackEvent.info("Window content initialization thread completed");
                },
                () -> {
                    AppFileWatcher.init();
                    FileBridge.init();
                    BlobManager.init();
                    TerminalView.init();
                    TerminalLauncherManager.init();
                    TrackEvent.info("File/Watcher initialization thread completed");
                },
                () -> {
                    PlatformInit.init(true);
                    AppImages.init();
                    imagesLoaded.countDown();
                    SystemIconManager.init();
                    iconsLoaded.countDown();
                    TrackEvent.info("Platform initialization thread completed");
                },
                () -> {
                    BrowserIconManager.loadIfNecessary();
                    shellLoaded.await();
                    BrowserLocalFileSystem.init();
                    storageLoaded.await();
                    BrowserFullSessionModel.init();
                    browserLoaded.countDown();
                    TrackEvent.info("Browser initialization thread completed");
                });

        AppGreetingsDialog.showAndWaitIfNeeded();
        TrackEvent.info("Waiting for startup dialogs to close");
        AppDialog.waitForAllDialogsClose();
        UpdateChangelogAlert.showIfNeeded();

        ActionProvider.initProviders();
        DataStoreProviders.init();

        AppConfigurationDialog.showIfNeeded();
        AppGnomeScaleDialog.showIfNeeded();

        TrackEvent.info("Finished base components initialization");
        initialized = true;
    }

    @Override
    public void onSwitchFrom() {}

    @Override
    public void finalTeardown() throws Exception {
        TrackEvent.withInfo("Base mode shutdown started").build();
        AbstractAction.reset();
        DataStorage.reset();
        DataStorageSyncHandler.getInstance().reset();
        SshLocalBridge.reset();
        BrowserFullSessionModel.DEFAULT.reset();
        LocalShell.reset(false);
        BrowserLocalFileSystem.reset();
        ProcessControlProvider.get().reset();
        AppPrefs.reset();
        AppBeaconServer.reset();
        KeePassXcPasswordManager.reset();
        StoreViewState.reset();
        AppLayoutModel.reset();
        AppTheme.reset();
        PlatformState.teardown();
        DataStoreProviders.reset();
        AppResources.reset();
        AppExtensionManager.reset();
        AppDataLock.unlock();
        BlobManager.reset();
        FileBridge.reset();
        GlobalTimer.reset();
        TrackEvent.info("Base mode shutdown finished");
    }
}
