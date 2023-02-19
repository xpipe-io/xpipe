package io.xpipe.app.browser;

import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.FileSystem;
import io.xpipe.core.store.ShellStore;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;

@Getter
public class BrowserModel {

    public static final BrowserModel DEFAULT = new BrowserModel();

    private final ObservableList<OpenFileSystemModel> openFileSystems = FXCollections.observableArrayList();
    private final Property<OpenFileSystemModel> selected = new SimpleObjectProperty<>();

    public OpenFileSystemModel getOpenModelFor(DataStore store) {
        return openFileSystems.stream()
                .filter(model -> model.getStore().equals(store))
                .findFirst()
                .orElseThrow();
    }

    public OpenFileSystemModel getOpenModelFor(FileSystem fileSystem) {
        return openFileSystems.stream()
                .filter(model -> model.getFileSystem().equals(fileSystem))
                .findFirst()
                .orElseThrow();
    }

    public void closeFileSystem(OpenFileSystemModel open) {
        ThreadHelper.runAsync(() -> {
            open.closeSync();
            openFileSystems.remove(open);
        });
    }

    public void openFileSystem(ShellStore store) {
        var found = openFileSystems.stream()
                .filter(fileSystemModel -> fileSystemModel.getStore().equals(store))
                .findFirst();
        if (found.isPresent()) {
            selected.setValue(found.get());
            return;
        }

        var model = new OpenFileSystemModel();
        openFileSystems.add(model);
        selected.setValue(model);
        model.switchAsync(store);
    }
}
