package io.xpipe.app.browser;

import io.xpipe.app.fxcomps.util.BindingsHelper;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.BooleanScope;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.store.FileStore;
import io.xpipe.core.store.FileSystemStore;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

@Getter
public class BrowserModel {

    public static final BrowserModel DEFAULT = new BrowserModel(Mode.BROWSER);
    private final Mode mode;
    private final ObservableList<OpenFileSystemModel> openFileSystems = FXCollections.observableArrayList();
    private final Property<OpenFileSystemModel> selected = new SimpleObjectProperty<>();
    private final BrowserTransferModel localTransfersStage = new BrowserTransferModel(this);
    private final ObservableList<BrowserEntry> selection = FXCollections.observableArrayList();
    @Setter
    private Consumer<List<FileStore>> onFinish;

    public BrowserModel(Mode mode) {
        this.mode = mode;

        selected.addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                return;
            }

            BindingsHelper.bindContent(selection, newValue.getFileList().getSelection());
        });
    }

    public void restoreState(BrowserSavedState state) {
        state.getLastSystems().forEach(e -> {
            restoreState(e, null);
        });
    }

    public void restoreState(BrowserSavedState.Entry e, BooleanProperty busy) {
        var storageEntry = DataStorage.get().getStoreEntryIfPresent(e.getUuid());
        storageEntry.ifPresent(entry -> {
            openFileSystemAsync(entry.ref(), e.getPath(), busy);
        });
    }

    public void reset() {
        var list = new ArrayList<BrowserSavedState.Entry>();
        openFileSystems.forEach(model -> {
            if (DataStorage.get().getStoreEntries().contains(model.getEntry().get())) {
                list.add(new BrowserSavedState.Entry(model.getEntry().get().getUuid(), model.getCurrentPath().get()));
            }
        });

        // Don't override state if it is empty
        if (list.size() == 0) {
            return;
        }

        var meaningful = list.size() > 1 || list.stream().allMatch(s -> s.getPath() != null);
        if (!meaningful) {
            return;
        }

        var state = BrowserSavedState.builder().lastSystems(list).build();
        state.save();
    }

    public void finishChooser() {
        if (!getMode().isChooser()) {
            throw new IllegalStateException();
        }

        var chosen = new ArrayList<>(selection);
        for (OpenFileSystemModel openFileSystem : openFileSystems) {
            closeFileSystemAsync(openFileSystem);
        }

        if (chosen.size() == 0) {
            return;
        }

        var stores = chosen.stream().map(
                entry -> new FileStore(entry.getRawFileEntry().getFileSystem().getStore(), entry.getRawFileEntry().getPath())).toList();
        onFinish.accept(stores);
    }

    public void closeFileSystemAsync(OpenFileSystemModel open) {
        ThreadHelper.runAsync(() -> {
            if (Objects.equals(selected.getValue(), open)) {
                selected.setValue(null);
            }
            open.closeSync();
            synchronized (BrowserModel.this) {
                openFileSystems.remove(open);
            }
        });
    }

    public void openExistingFileSystemIfPresent(DataStoreEntryRef<? extends FileSystemStore> store) {
        var found = openFileSystems.stream().filter(model -> Objects.equals(model.getEntry(), store)).findFirst();
        if (found.isPresent()) {
            selected.setValue(found.get());
        } else {
            openFileSystemAsync(store, null, null);
        }
    }

    public void openFileSystemAsync(DataStoreEntryRef<? extends FileSystemStore> store, String path, BooleanProperty externalBusy) {
        //        // Prevent multiple tabs in non browser modes
        //        if (!mode.equals(Mode.BROWSER)) {
        //            ThreadHelper.runFailableAsync(() -> {
        //                var open = openFileSystems.size() > 0 ? openFileSystems.get(0) : null;
        //                if (open != null) {
        //                    open.closeSync();
        //                    openFileSystems.remove(open);
        //                }
        //
        //                var model = new OpenFileSystemModel(this, store);
        //                openFileSystems.add(model);
        //                selected.setValue(model);
        //                model.switchSync(store);
        //            });
        //            return;
        //        }

        if (store == null) {
            return;
        }

        ThreadHelper.runFailableAsync(() -> {
            OpenFileSystemModel model;

            try (var b = new BooleanScope(externalBusy != null ? externalBusy : new SimpleBooleanProperty()).start()) {
                // Prevent multiple calls from interfering with each other
                synchronized (BrowserModel.this) {
                    model = new OpenFileSystemModel(this, store);
                    model.initFileSystem();
                    model.initSavedState();
                }

                openFileSystems.add(model);
                selected.setValue(model);
            }
            if (path != null) {
                model.initWithGivenDirectory(path);
            } else {
                model.initWithDefaultDirectory();
            }
        });
    }

    @Getter
    public enum Mode {
        BROWSER(false, true, true, true),
        SINGLE_FILE_CHOOSER(true, false, true, false),
        SINGLE_FILE_SAVE(true, false, true, false),
        MULTIPLE_FILE_CHOOSER(true, true, true, false),
        SINGLE_DIRECTORY_CHOOSER(true, false, false, true),
        MULTIPLE_DIRECTORY_CHOOSER(true, true, false, true);

        private final boolean chooser;
        private final boolean multiple;
        private final boolean acceptsFiles;
        private final boolean acceptsDirectories;

        Mode(boolean chooser, boolean multiple, boolean acceptsFiles, boolean acceptsDirectories) {
            this.chooser = chooser;
            this.multiple = multiple;
            this.acceptsFiles = acceptsFiles;
            this.acceptsDirectories = acceptsDirectories;
        }
    }
}
