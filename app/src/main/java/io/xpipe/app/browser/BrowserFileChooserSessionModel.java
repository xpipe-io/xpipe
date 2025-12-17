package io.xpipe.app.browser;

import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import io.xpipe.app.ext.FileEntry;
import io.xpipe.app.ext.FileSystem;
import io.xpipe.app.ext.FileSystemStore;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.BooleanScope;
import io.xpipe.app.util.FileReference;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.FailableFunction;
import io.xpipe.core.FilePath;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Getter
public class BrowserFileChooserSessionModel extends BrowserAbstractSessionModel<BrowserFileSystemTabModel> {

    private final ObservableList<BrowserEntry> fileSelection = FXCollections.observableArrayList();
    private final boolean directory;

    @Setter
    private Consumer<List<FileReference>> onFinish;

    public BrowserFileChooserSessionModel(boolean directory) {
        this.directory = directory;
        selectedEntry.addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                fileSelection.clear();
                return;
            }

            fileSelection.setAll(newValue.getFileList().getSelection());
            newValue.getFileList().getSelection().addListener((ListChangeListener<? super BrowserEntry>) c -> {
                fileSelection.setAll(newValue.getFileList().getSelection());
            });
        });
    }

    public void finishChooser() {
        var chosen = new ArrayList<>(fileSelection.stream().map(be -> be.getRawFileEntry().getPath()).toList());

        synchronized (BrowserFileChooserSessionModel.this) {
            var open = selectedEntry.getValue();
            if (open != null) {
                if (chosen.isEmpty() && directory) {
                    var current = open.getCurrentDirectory();
                    if (current != null) {
                        chosen.add(current.getPath());
                    }
                }

                ThreadHelper.runAsync(() -> {
                    open.close();
                });
            }
        }

        var stores = chosen.stream()
                .map(entry -> new FileReference(
                        selectedEntry.getValue().getEntry(),
                        entry))
                .toList();
        onFinish.accept(stores);
    }

    public void closeFileSystem() {
        synchronized (BrowserFileChooserSessionModel.this) {
            var open = selectedEntry.getValue();
            if (open != null) {
                ThreadHelper.runAsync(() -> {
                    open.close();
                });
            }
        }
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
            BrowserFileSystemTabModel model;

            try (var ignored =
                    new BooleanScope(externalBusy != null ? externalBusy : new SimpleBooleanProperty()).start()) {
                model = new BrowserFileSystemTabModel(
                        this,
                        store,
                        customFileSystemFactory != null
                                ? customFileSystemFactory
                                : ref -> ref.getStore().createFileSystem());
                model.init();
                // Prevent multiple calls from interfering with each other
                synchronized (BrowserFileChooserSessionModel.this) {
                    selectedEntry.setValue(model);
                    sessionEntries.add(model);
                }

                if (path != null) {
                    var initialPath = path.apply(model);
                    if (initialPath != null) {
                        model.initWithGivenDirectory(initialPath.toDirectory());
                        return;
                    }
                }
                model.initWithDefaultDirectory();
            }
        });
    }
}
