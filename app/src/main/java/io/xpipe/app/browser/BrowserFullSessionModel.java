package io.xpipe.app.browser;

import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import io.xpipe.app.browser.file.BrowserHistorySavedState;
import io.xpipe.app.browser.file.BrowserHistoryTabModel;
import io.xpipe.app.browser.file.BrowserTransferModel;
import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.ext.FileSystem;
import io.xpipe.app.ext.FileSystemStore;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.BooleanScope;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.FailableFunction;
import io.xpipe.core.FilePath;

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
    private final BrowserTransferModel localTransfersStage = new BrowserTransferModel(this);
    private final Property<Boolean> draggingFiles = new SimpleBooleanProperty();
    private final Property<BrowserSessionTab> globalPinnedTab = new SimpleObjectProperty<>();
    private final ObservableMap<BrowserSessionTab, BrowserSessionTab> splits = FXCollections.observableHashMap();
    private final ObservableValue<BrowserSessionTab> effectiveRightTab = createEffectiveRightTab();
    private final SequencedSet<BrowserSessionTab> previousTabs = new LinkedHashSet<>();

    public BrowserFullSessionModel() {
        sessionEntries.addListener((ListChangeListener<? super BrowserSessionTab>) c -> {
            var v = globalPinnedTab.getValue();
            if (v != null && !c.getList().contains(v)) {
                globalPinnedTab.setValue(null);
            }

            splits.keySet().removeIf(browserSessionTab -> !c.getList().contains(browserSessionTab));
        });

        selectedEntry.addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                previousTabs.remove(newValue);
                previousTabs.add(newValue);
            }
        });
    }

    public static void init() throws Exception {
        DEFAULT.openSync(new BrowserHistoryTabModel(DEFAULT), null);
        if (AppPrefs.get().pinLocalMachineOnStartup().get()) {
            var tab = new BrowserFileSystemTabModel(
                    DEFAULT,
                    DataStorage.get().local().ref(),
                    BrowserFileSystemTabModel.SelectionMode.ALL,
                    ref -> ref.getStore().createFileSystem());
            try {
                DEFAULT.openSync(tab, null);
                DEFAULT.pinTab(tab);
            } catch (Exception ex) {
                // Don't fail startup if this operation fails
                ErrorEventFactory.fromThrowable(ex).handle();
            }
        }
    }

    private ObservableValue<BrowserSessionTab> createEffectiveRightTab() {
        return Bindings.createObjectBinding(
                () -> {
                    var current = selectedEntry.getValue();
                    if (current == null) {
                        return null;
                    }

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

        var previousOthers = previousTabs.stream()
                .filter(browserSessionTab -> browserSessionTab != tab && browserSessionTab.isCloseable())
                .toList();
        if (previousOthers.size() > 0) {
            var prev = previousOthers.getLast();
            getSelectedEntry().setValue(prev);
        }
    }

    public void unpinTab() {
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
            openFileSystemAsync(entry.ref(), null, model -> e.getPath(), busy);
        });
    }

    public void reset() {
        synchronized (BrowserFullSessionModel.this) {
            if (globalPinnedTab.getValue() != null) {
                globalPinnedTab.setValue(null);
            }

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
            if (all.size() > 0) {
                ThreadHelper.sleep(1000);
            }
        }

        // Delete all files
        localTransfersStage.clear(true);
    }

    public void openFileSystemAsync(
            DataStoreEntryRef<? extends FileSystemStore> store,
            FailableFunction<DataStoreEntryRef<FileSystemStore>, FileSystem, Exception> customFileSystemFactory,
            FailableFunction<BrowserFileSystemTabModel, FilePath, Exception> path,
            BooleanProperty externalBusy) {
        if (store == null) {
            return;
        }

        ThreadHelper.runFailableAsync(() -> {
            openFileSystemSync(store, customFileSystemFactory, path, externalBusy, true);
        });
    }

    public BrowserFileSystemTabModel openFileSystemSync(
            DataStoreEntryRef<? extends FileSystemStore> store,
            FailableFunction<DataStoreEntryRef<FileSystemStore>, FileSystem, Exception> customFileSystemFactory,
            FailableFunction<BrowserFileSystemTabModel, FilePath, Exception> path,
            BooleanProperty externalBusy,
            boolean select)
            throws Exception {
        BrowserFileSystemTabModel model;
        try (var ignored =
                new BooleanScope(externalBusy != null ? externalBusy : new SimpleBooleanProperty()).start()) {
            try (var ignored2 = new BooleanScope(busy).exclusive().start()) {
                model = new BrowserFileSystemTabModel(
                        this,
                        store,
                        BrowserFileSystemTabModel.SelectionMode.ALL,
                        customFileSystemFactory != null
                                ? customFileSystemFactory
                                : ref -> ref.getStore().createFileSystem());
                model.init();
                // Prevent multiple calls from interfering with each other
                synchronized (BrowserFullSessionModel.this) {
                    sessionEntries.add(model);
                    if (select) {
                        AppLayoutModel.get().selectBrowser();
                        // The tab pane doesn't automatically select new tabs
                        selectedEntry.setValue(model);
                    }
                }
            }
        }
        if (path != null) {
            var applied = path.apply(model);
            if (applied != null) {
                model.initWithGivenDirectory(applied.toDirectory());
            } else {
                model.initWithDefaultDirectory();
            }
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
        previousTabs.remove(e);
        super.closeSync(e);
    }
}
