package io.xpipe.app.core.mode;

import io.xpipe.app.comp.storage.collection.SourceCollectionViewState;
import io.xpipe.app.comp.storage.store.StoreViewState;
import io.xpipe.app.core.*;
import io.xpipe.app.issue.*;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.util.DefaultSecretValue;
import io.xpipe.app.util.FileBridge;
import io.xpipe.app.util.LockedSecretValue;
import io.xpipe.core.impl.LocalStore;
import io.xpipe.core.util.JacksonMapper;

public class BaseMode extends OperationMode {

    @Override
    public boolean isSupported() {
        return true;
    }

    @Override
    public String getId() {
        return "background";
    }

    @Override
    public void onSwitchTo() {}

    @Override
    public void onSwitchFrom() {}

    @Override
    public void initialSetup() throws Exception {
        TrackEvent.info("mode", "Initializing base mode components ...");
        AppExtensionManager.init(true);
        JacksonMapper.initModularized(AppExtensionManager.getInstance().getExtendedLayer());
        JacksonMapper.configure(objectMapper -> {
            objectMapper.registerSubtypes(LockedSecretValue.class, DefaultSecretValue.class);
        });
        LocalStore.init();
        AppPrefs.init();
        AppCharsets.init();
        AppCharsetter.init();
        DataStorage.init();
        AppFileWatcher.init();
        FileBridge.init();
        AppSocketServer.init();
        TrackEvent.info("mode", "Finished base components initialization");
    }

    @Override
    public void finalTeardown() {
        TrackEvent.info("mode", "Background mode shutdown started");
        AppSocketServer.reset();
        SourceCollectionViewState.reset();
        StoreViewState.reset();
        DataStorage.reset();
        AppPrefs.reset();
        AppExtensionManager.reset();
        TrackEvent.info("mode", "Background mode shutdown finished");
    }

    @Override
    public ErrorHandler getErrorHandler() {
        var log = new LogErrorHandler();
        return new SyncErrorHandler(event -> {
            log.handle(event);
            ErrorAction.ignore().handle(event);
        });
    }
}
