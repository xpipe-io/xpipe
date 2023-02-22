package io.xpipe.app.browser;

import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.store.ShellStore;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;

@Getter
public class FileBrowserModel {

    public static final FileBrowserModel DEFAULT = new FileBrowserModel();

    private final ObservableList<OpenFileSystemModel> openFileSystems = FXCollections.observableArrayList();
    private final Property<OpenFileSystemModel> selected = new SimpleObjectProperty<>();

    public void closeFileSystem(OpenFileSystemModel open) {
        ThreadHelper.runAsync(() -> {
            open.closeSync();
            openFileSystems.remove(open);
        });
    }

    public void openFileSystem(ShellStore store) {
        var found = openFileSystems.stream()
                .filter(fileSystemModel -> fileSystemModel.getStore().getValue().equals(store))
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
