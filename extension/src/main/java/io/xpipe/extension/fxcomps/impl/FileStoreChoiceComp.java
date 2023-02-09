package io.xpipe.extension.fxcomps.impl;

import io.xpipe.core.impl.FileStore;
import io.xpipe.core.impl.LocalStore;
import io.xpipe.core.store.FileSystemStore;
import io.xpipe.core.store.MachineStore;
import io.xpipe.extension.I18n;
import io.xpipe.extension.fxcomps.SimpleComp;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.List;

public class FileStoreChoiceComp extends SimpleComp {

    private final List<MachineStore> availableFileSystems;
    private final Property<FileStore> selected;

    public FileStoreChoiceComp(List<MachineStore> availableFileSystems, Property<FileStore> selected) {
        this.availableFileSystems = availableFileSystems;
        this.selected = selected;
    }

    private void setSelected(FileSystemStore fileSystemStore, String file) {
        selected.setValue(fileSystemStore != null && file != null ? new FileStore(fileSystemStore, file) : null);
    }

    @Override
    protected Region createSimple() {
        var fileProperty = new SimpleStringProperty(
                selected.getValue() != null ? selected.getValue().getFile() : null);
        var fileSystemProperty = new SimpleObjectProperty<>(
                selected.getValue() != null ? selected.getValue().getFileSystem() : availableFileSystems.get(0));

        fileProperty.addListener((observable, oldValue, newValue) -> {
            setSelected(fileSystemProperty.get(), fileProperty.get());
        });
        fileSystemProperty.addListener((observable, oldValue, newValue) -> {
            setSelected(fileSystemProperty.get(), fileProperty.get());
        });

        var fileSystemChoiceComp = new FileSystemStoreChoiceComp(fileSystemProperty);
        if (availableFileSystems.size() == 1) {
            fileSystemChoiceComp.hide(new SimpleBooleanProperty(true));
        }

        var fileNameComp = new TextFieldComp(fileProperty).apply(struc -> HBox.setHgrow(struc.get(), Priority.ALWAYS));
        var fileBrowseButton = new IconButtonComp("mdi2f-folder-open-outline", () -> {
                    if (fileSystemProperty.get() != null && fileSystemProperty.get() instanceof LocalStore) {
                        var fileChooser = createChooser();
                        File file = fileChooser.showOpenDialog(null);
                        if (file != null && file.exists()) {
                            fileProperty.setValue(file.toString());
                        }
                    }
                })
                .hide(fileSystemProperty.isNotEqualTo(new LocalStore()));

        var layout = new HorizontalComp(List.of(fileSystemChoiceComp, fileNameComp, fileBrowseButton));

        return layout.createRegion();
    }

    private FileChooser createChooser() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(I18n.get("browseFileTitle"));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(I18n.get("anyFile"), "*"));
        return fileChooser;
    }
}
