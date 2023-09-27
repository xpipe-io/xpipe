package io.xpipe.app.fxcomps.impl;

import io.xpipe.app.ext.DataStoreProviders;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.util.CustomComboBoxBuilder;
import io.xpipe.core.store.FileSystemStore;
import javafx.beans.property.Property;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;

public class FileSystemStoreChoiceComp extends SimpleComp {

    private final Property<FileSystemStore> selected;

    public FileSystemStoreChoiceComp(Property<FileSystemStore> selected) {
        this.selected = selected;
    }

    private static String getName(FileSystemStore store) {
        return DataStorage.get().getUsableStores().stream()
                .filter(e -> e.equals(store))
                .findAny()
                .map(e -> DataStorage.get().getStoreDisplayName(e).orElse("?"))
                .orElse("?");
    }

    private Region createGraphic(FileSystemStore s) {
        var provider = DataStoreProviders.byStore(s);
        var img = PrettyImageHelper.ofFixedSquare(provider.getDisplayIconFileName(s), 16);
        return new Label(getName(s), img.createRegion());
    }

    private Region createDisplayGraphic(FileSystemStore s) {
        var provider = DataStoreProviders.byStore(s);
        var img = PrettyImageHelper.ofFixedSquare(provider.getDisplayIconFileName(s), 16);
        return new Label(null, img.createRegion());
    }

    @Override
    protected Region createSimple() {
        var comboBox = new CustomComboBoxBuilder<>(selected, this::createGraphic, null, v -> true);
        comboBox.setAccessibleNames(store -> getName(store));
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
