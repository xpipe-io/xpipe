package io.xpipe.app.browser;

import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import io.xpipe.app.browser.file.BrowserHistorySavedState;
import io.xpipe.app.browser.file.BrowserHistorySavedStateImpl;
import io.xpipe.app.browser.file.BrowserHistoryTabModel;
import io.xpipe.app.browser.file.BrowserTransferModel;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.BooleanScope;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.store.FileNames;
import io.xpipe.core.store.FileSystemStore;
import io.xpipe.core.util.FailableFunction;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableMap;

import lombok.Getter;

import java.util.*;

@Getter
public class BrowserFullSessionModel extends BrowserAbstractSessionModel<BrowserSessionTab> {

    public static final BrowserFullSessionModel DEFAULT = new BrowserFullSessionModel();

    static {
        DEFAULT.getSessionEntries().add(new BrowserHistoryTabModel(DEFAULT));
    }

    private final BrowserTransferModel localTransfersStage = new BrowserTransferModel(this);
    private final Property<Boolean> draggingFiles = new SimpleBooleanProperty();
    private final Property<BrowserSessionTab> globalPinnedTab = new SimpleObjectProperty<>();
    private final ObservableMap<BrowserSessionTab, BrowserSessionTab> splits = FXCollections.observableHashMap();
    private final ObservableValue<BrowserSessionTab> effectiveRightTab = createEffectiveRightTab();

    private ObservableValue<BrowserSessionTab> createEffectiveRightTab() {
        return Bindings.createObjectBinding(
                () -> {
                    var current = selectedEntry.getValue();
                    if (!current.isCloseable()) {
                        return null;
                    }

                    var split = splits.get(current);
                    if (split != null) {
                        return split;
                    }

                    var global = globalPinnedTab.getValue();
                    if (global == null) {
                        return null;
                    }

                    if (global == selectedEntry.getValue()) {
                        return null;
                    }

                    return global;
                },
                globalPinnedTab,
                selectedEntry,
                splits);
    }

    public BrowserFullSessionModel() {
        sessionEntries.addListener((ListChangeListener<? super BrowserSessionTab>) c -> {
            var v = globalPinnedTab.getValue();
            if (v != null && !c.getList().contains(v)) {
                globalPinnedTab.setValue(null);
            }

            splits.keySet().removeIf(browserSessionTab -> !c.getList().contains(browserSessionTab));
        });
    }

    public Set<BrowserSessionTab> getAllTabs() {
        var set = new HashSet<BrowserSessionTab>();
        set.addAll(sessionEntries);
        set.addAll(splits.values());
        if (globalPinnedTab.getValue() != null) {
            set.add(globalPinnedTab.getValue());
        }
        return set;
    }

    public void splitTab(BrowserSessionTab tab, BrowserSessionTab split) {
        if (splits.containsKey(tab)) {
            return;
        }

        splits.put(tab, split);
        ThreadHelper.runFailableAsync(() -> {
            split.init();
        });
    }

    public void unsplitTab(BrowserSessionTab tab) {
        if (splits.values().remove(tab)) {
            ThreadHelper.runFailableAsync(() -> {
                tab.close();
            });
        }
    }

    public void pinTab(BrowserSessionTab tab) {
        if (tab.equals(globalPinnedTab.getValue())) {
            return;
        }

        globalPinnedTab.setValue(tab);

        var nextIndex = getSessionEntries().indexOf(tab) + 1;
        if (nextIndex < getSessionEntries().size()) {
            getSelectedEntry().setValue(getSessionEntries().get(nextIndex));
        }
    }

    public void unpinTab(BrowserSessionTab tab) {
        ThreadHelper.runFailableAsync(() -> {
            globalPinnedTab.setValue(null);
        });
    }

    public void restoreState(BrowserHistorySavedState state) {
        ThreadHelper.runAsync(() -> {
            var l = new ArrayList<>(state.getEntries());
            l.forEach(e -> {
                restoreStateAsync(e, null);
                // Don't try to run everything in parallel as that can be taxing
                ThreadHelper.sleep(1000);
            });
        });
    }

    public void restoreStateAsync(BrowserHistorySavedState.Entry e, BooleanProperty busy) {
        var storageEntry = DataStorage.get().getStoreEntryIfPresent(e.getUuid());
        storageEntry.ifPresent(entry -> {
            openFileSystemAsync(entry.ref(), model -> e.getPath(), busy);
        });
    }

    public void reset() {
        synchronized (BrowserFullSessionModel.this) {
            var all = new ArrayList<>(sessionEntries);
            for (var o : all) {
                // Don't close busy connections gracefully
                // as we otherwise might lock up
                if (!o.canImmediatelyClose()) {
                    continue;
                }

                // Prevent blocking of shutdown
                closeAsync(o);
            }
            BrowserHistorySavedStateImpl.get().save();
        }

        // Delete all files
        localTransfersStage.clear(true);
    }

    public void openFileSystemAsync(
            DataStoreEntryRef<? extends FileSystemStore> store,
            FailableFunction<BrowserFileSystemTabModel, String, Exception> path,
            BooleanProperty externalBusy) {
        if (store == null) {
            return;
        }

        ThreadHelper.runFailableAsync(() -> {
            openFileSystemSync(store, path, externalBusy, true);
        });
    }

    public BrowserFileSystemTabModel openFileSystemSync(
            DataStoreEntryRef<? extends FileSystemStore> store,
            FailableFunction<BrowserFileSystemTabModel, String, Exception> path,
            BooleanProperty externalBusy,
            boolean select)
            throws Exception {
        BrowserFileSystemTabModel model;
        try (var b = new BooleanScope(externalBusy != null ? externalBusy : new SimpleBooleanProperty()).start()) {
            try (var sessionBusy = new BooleanScope(busy).exclusive().start()) {
                model = new BrowserFileSystemTabModel(this, store, BrowserFileSystemTabModel.SelectionMode.ALL);
                model.init();
                // Prevent multiple calls from interfering with each other
                synchronized (BrowserFullSessionModel.this) {
                    sessionEntries.add(model);
                    if (select) {
                        // The tab pane doesn't automatically select new tabs
                        selectedEntry.setValue(model);
                    }
                }
            }
        }
        if (path != null) {
            model.initWithGivenDirectory(FileNames.toDirectory(path.apply(model)));
        } else {
            model.initWithDefaultDirectory();
        }
        return model;
    }

    @Override
    public void closeSync(BrowserSessionTab e) {
        var split = splits.get(e);
        if (split != null) {
            split.close();
        }

        super.closeSync(e);
    }
}
