package io.xpipe.app.fxcomps.impl;

import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.CustomComboBoxBuilder;
import io.xpipe.core.store.FileSystemStore;
import javafx.beans.property.Property;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;

public class FileSystemStoreChoiceComp extends SimpleComp {

    private final Property<DataStoreEntryRef<? extends FileSystemStore>>   selected;

    public FileSystemStoreChoiceComp(Property<DataStoreEntryRef<? extends FileSystemStore>>  selected) {
        this.selected = selected;
    }

    private static String getName(DataStoreEntryRef<? extends FileSystemStore> store) {
        return store.get().getName();
    }

    private Region createGraphic(DataStoreEntryRef<? extends FileSystemStore> s) {
        var provider = s.get().getProvider();
        var img = PrettyImageHelper.ofFixedSquare(provider.getDisplayIconFileName(s.getStore()), 16);
        return new Label(getName(s), img.createRegion());
    }

    private Region createDisplayGraphic(DataStoreEntryRef<? extends FileSystemStore> s) {
        var provider = s.get().getProvider();
        var img = PrettyImageHelper.ofFixedSquare(provider.getDisplayIconFileName(s.getStore()), 16);
        return new Label(null, img.createRegion());
    }

    @Override
    protected Region createSimple() {
        var comboBox = new CustomComboBoxBuilder<>(selected, this::createGraphic, null, v -> true);
        comboBox.setAccessibleNames(FileSystemStoreChoiceComp::getName);
        comboBox.setSelectedDisplay(this::createDisplayGraphic);
        DataStorage.get().getUsableEntries().stream()
                .filter(e -> e.getStore() instanceof FileSystemStore)
                .map(DataStoreEntry::<FileSystemStore>ref)
                .forEach(comboBox::add);
        ComboBox<Node> cb = comboBox.build();
        cb.getStyleClass().add("choice-comp");
        cb.setMaxWidth(45);
        return cb;
    }
}
