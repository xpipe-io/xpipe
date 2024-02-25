package io.xpipe.app.browser;

import io.xpipe.app.fxcomps.util.BindingsHelper;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.BooleanScope;
import io.xpipe.app.util.FileReference;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.store.FileNames;
import io.xpipe.core.store.FileSystemStore;
import io.xpipe.core.util.FailableFunction;
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
import java.util.function.Consumer;

@Getter
public class BrowserModel {

    public static final BrowserModel DEFAULT = new BrowserModel(Mode.BROWSER, BrowserSavedStateImpl.load());

    private final Mode mode;
    private final ObservableList<OpenFileSystemModel> openFileSystems = FXCollections.observableArrayList();
    private final Property<OpenFileSystemModel> selected = new SimpleObjectProperty<>();
    private final BrowserTransferModel localTransfersStage = new BrowserTransferModel(this);
    private final ObservableList<BrowserEntry> selection = FXCollections.observableArrayList();
    private final BrowserSavedState savedState;

    @Setter
    private Consumer<List<FileReference>> onFinish;

    public BrowserModel(Mode mode, BrowserSavedState savedState) {
        this.mode = mode;
        this.savedState = savedState;

        selected.addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                selection.clear();
                return;
            }

            BindingsHelper.bindContent(selection, newValue.getFileList().getSelection());
        });
    }

    public void restoreState(BrowserSavedState state) {
        ThreadHelper.runAsync(() -> {
            state.getEntries().forEach(e -> {
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
        synchronized (BrowserModel.this) {
            for (OpenFileSystemModel o : new ArrayList<>(openFileSystems)) {
                // Don't close busy connections gracefully
                // as we otherwise might lock up
                if (o.isBusy()) {
                    continue;
                }

                closeFileSystemSync(o);
            }
            if (savedState != null) {
                savedState.save();
            }
        }

        // Delete all files
        localTransfersStage.clear();
    }

    public void finishChooser() {
        if (!getMode().isChooser()) {
            throw new IllegalStateException();
        }

        var chosen = new ArrayList<>(selection);

        synchronized (BrowserModel.this) {
            for (OpenFileSystemModel openFileSystem : openFileSystems) {
                closeFileSystemAsync(openFileSystem);
            }
        }

        if (chosen.size() == 0) {
            return;
        }

        var stores = chosen.stream()
                .map(entry -> new FileReference(
                        selected.getValue().getEntry(), entry.getRawFileEntry().getPath()))
                .toList();
        onFinish.accept(stores);
    }

    public void closeFileSystemAsync(OpenFileSystemModel open) {
        ThreadHelper.runAsync(() -> {
            closeFileSystemSync(open);
        });
    }

    private void closeFileSystemSync(OpenFileSystemModel open) {
        if (DataStorage.get().getStoreEntries().contains(open.getEntry().get())
                && savedState != null
                && open.getCurrentPath().get() != null) {
            savedState.add(new BrowserSavedState.Entry(
                    open.getEntry().get().getUuid(), open.getCurrentPath().get()));
        }
        open.closeSync();
        synchronized (BrowserModel.this) {
            openFileSystems.remove(open);
        }
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
                model = new OpenFileSystemModel(this, store);
                model.initFileSystem();
                model.initSavedState();
                // Prevent multiple calls from interfering with each other
                synchronized (BrowserModel.this) {
                    openFileSystems.add(model);
                    // The tab pane doesn't automatically select new tabs
                    selected.setValue(model);
                }
            }
            if (path != null) {
                model.initWithGivenDirectory(FileNames.toDirectory(path.apply(model)));
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
