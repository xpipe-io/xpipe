package io.xpipe.app.browser;

import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.impl.FileStore;
import io.xpipe.core.store.ShellStore;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

@Getter
public class FileBrowserModel {

    public FileBrowserModel(Mode mode) {
        this.mode = mode;
    }

    public static enum Mode {
        BROWSER,
        SINGLE_FILE_CHOOSER,
        SINGLE_FILE_SAVE,
        MULTIPLE_FILE_CHOOSER,
        DIRECTORY_CHOOSER
    }

    public static final FileBrowserModel DEFAULT = new FileBrowserModel(Mode.BROWSER);

    private final Mode mode;

    @Setter
    private Consumer<List<FileStore>> onFinish;

    private final ObservableList<OpenFileSystemModel> openFileSystems = FXCollections.observableArrayList();
    private final Property<OpenFileSystemModel> selected = new SimpleObjectProperty<>();
    private final LocalFileTransferStage localTransfersStage = new LocalFileTransferStage();

    public void finishChooser() {
        if (getMode().equals(Mode.BROWSER)) {
            throw new IllegalStateException();
        }

        var selectedFiles = openFileSystems.get(0).getFileList().getSelected();
        closeFileSystem(openFileSystems.get(0));

        if (selectedFiles.size() == 0) {
            return;
        }
        var stores = selectedFiles.stream()
                .map(entry -> new FileStore(
                        entry.getRawFileEntry().getFileSystem().getStore(),
                        entry.getRawFileEntry().getPath()))
                .toList();
        onFinish.accept(stores);
    }

    public void closeFileSystem(OpenFileSystemModel open) {
        ThreadHelper.runAsync(() -> {
            if (Objects.equals(selected.getValue(), open)) {
                selected.setValue(null);
            }
            open.closeSync();
            openFileSystems.remove(open);
        });
    }

    public void openExistingFileSystemIfPresent(ShellStore store) {
        var found = openFileSystems.stream()
                .filter(model -> Objects.equals(model.getStore(), store))
                .findFirst();
        if (found.isPresent()) {
            selected.setValue(found.get());
        } else {
            openFileSystemAsync(store, null);
        }
    }

    public void openFileSystemAsync(ShellStore store, String path) {
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

        ThreadHelper.runFailableAsync(() -> {
            var model = new OpenFileSystemModel(this, store);
            model.initFileSystem();
            openFileSystems.add(model);
            selected.setValue(model);
            if (path != null) {
                model.cd(path);
            } else {
                model.initDirectory();
            }
        });
    }
}
