package io.xpipe.app.browser.session;

import io.xpipe.app.browser.BrowserSavedState;
import io.xpipe.app.browser.BrowserSavedStateImpl;
import io.xpipe.app.browser.BrowserTransferModel;
import io.xpipe.app.browser.fs.OpenFileSystemModel;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.BooleanScope;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.store.FileNames;
import io.xpipe.core.store.FileSystemStore;
import io.xpipe.core.util.FailableFunction;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import lombok.Getter;

import java.util.ArrayList;

@Getter
public class BrowserSessionModel extends BrowserAbstractSessionModel<BrowserSessionTab<?>> {

    public static final BrowserSessionModel DEFAULT = new BrowserSessionModel(BrowserSavedStateImpl.load());

    private final BrowserTransferModel localTransfersStage = new BrowserTransferModel(this);
    private final BrowserSavedState savedState;

    public BrowserSessionModel(BrowserSavedState savedState) {
        this.savedState = savedState;
    }

    public void restoreState(BrowserSavedState state) {
        ThreadHelper.runAsync(() -> {
            var l = new ArrayList<>(state.getEntries());
            l.forEach(e -> {
                restoreStateAsync(e, null);
                // Don't try to run everything in parallel as that can be taxing
                ThreadHelper.sleep(1000);
            });
        });
    }

    public void restoreStateAsync(BrowserSavedState.Entry e, BooleanProperty busy) {
        var storageEntry = DataStorage.get().getStoreEntryIfPresent(e.getUuid());
        storageEntry.ifPresent(entry -> {
            openFileSystemAsync(entry.ref(), model -> e.getPath(), busy);
        });
    }

    public void reset() {
        synchronized (BrowserSessionModel.this) {
            for (var o : new ArrayList<>(sessionEntries)) {
                // Don't close busy connections gracefully
                // as we otherwise might lock up
                if (o.canImmediatelyClose()) {
                    continue;
                }

                closeSync(o);
            }
            if (savedState != null) {
                savedState.save();
            }
        }

        // Delete all files
        localTransfersStage.clear();
    }

    public void openFileSystemAsync(
            DataStoreEntryRef<? extends FileSystemStore> store,
            FailableFunction<OpenFileSystemModel, String, Exception> path,
            BooleanProperty externalBusy) {
        if (store == null) {
            return;
        }

        ThreadHelper.runFailableAsync(() -> {
            OpenFileSystemModel model;

            try (var b = new BooleanScope(externalBusy != null ? externalBusy : new SimpleBooleanProperty()).start()) {
                model = new OpenFileSystemModel(this, store, OpenFileSystemModel.SelectionMode.ALL);
                model.init();
                // Prevent multiple calls from interfering with each other
                synchronized (BrowserSessionModel.this) {
                    sessionEntries.add(model);
                    // The tab pane doesn't automatically select new tabs
                    selectedEntry.setValue(model);
                }
            }
            if (path != null) {
                model.initWithGivenDirectory(FileNames.toDirectory(path.apply(model)));
            } else {
                model.initWithDefaultDirectory();
            }
        });
    }
}
