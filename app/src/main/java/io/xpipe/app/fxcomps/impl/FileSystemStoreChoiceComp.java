package io.xpipe.app.fxcomps.impl;

import io.xpipe.app.ext.DataStoreProviders;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.util.CustomComboBoxBuilder;
import io.xpipe.app.util.XPipeDaemon;
import io.xpipe.core.impl.FileStore;
import io.xpipe.core.store.FileSystemStore;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;

public class FileSystemStoreChoiceComp extends SimpleComp {

    private final Property<FileStore> selected;

    public FileSystemStoreChoiceComp(Property<FileStore> selected) {
        this.selected = selected;
    }

    private static String getName(FileSystemStore store) {
        var name = DataStorage.get().getUsableStores().stream()
                .filter(e -> e.equals(store))
                .findAny()
                .map(e -> XPipeDaemon.getInstance().getStoreName(e).orElse("?"))
                .orElse("?");
        return name;
    }

    private Region createGraphic(FileSystemStore s) {
        var provider = DataStoreProviders.byStore(s);
        var img = new PrettyImageComp(new SimpleStringProperty(provider.getDisplayIconFileName()), 16, 16);
        return new Label(getName(s), img.createRegion());
    }

    private Region createDisplayGraphic(FileSystemStore s) {
        var provider = DataStoreProviders.byStore(s);
        var img = new PrettyImageComp(new SimpleStringProperty(provider.getDisplayIconFileName()), 16, 16);
        return new Label(null, img.createRegion());
    }

    @Override
    protected Region createSimple() {
        var fileSystemProperty = new SimpleObjectProperty<>(
                selected.getValue() != null ? selected.getValue().getFileSystem() : null);
        fileSystemProperty.addListener((observable, oldValue, newValue) -> {
            selected.setValue(FileStore.builder()
                    .fileSystem(newValue)
                    .file(selected.getValue() != null ? selected.getValue().getFile() : null)
                    .build());
        });

        selected.addListener((observable, oldValue, newValue) -> {
            fileSystemProperty.setValue(newValue.getFileSystem());
        });

        var comboBox =
                new CustomComboBoxBuilder<FileSystemStore>(fileSystemProperty, this::createGraphic, null, v -> true);
        comboBox.setSelectedDisplay(this::createDisplayGraphic);
        DataStorage.get().getUsableStores().stream()
                .filter(e -> e instanceof FileSystemStore)
                .map(e -> (FileSystemStore) e)
                .forEach(comboBox::add);
        ComboBox<Node> cb = comboBox.build();
        cb.getStyleClass().add("choice-comp");
        cb.setMaxWidth(45);
        return cb;
    }
}
