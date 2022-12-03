package io.xpipe.extension.fxcomps.impl;

import io.xpipe.core.impl.LocalStore;
import io.xpipe.core.store.FileSystemStore;
import io.xpipe.extension.DataStoreProviders;
import io.xpipe.extension.I18n;
import io.xpipe.extension.fxcomps.SimpleComp;
import io.xpipe.extension.util.CustomComboBoxBuilder;
import io.xpipe.extension.util.XPipeDaemon;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleStringProperty;
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
        var name = XPipeDaemon.getInstance().getNamedStores().stream()
                .filter(e -> e.equals(store))
                .findAny()
                .map(e -> XPipeDaemon.getInstance().getStoreName(e).orElse("?"))
                .orElse(I18n.get("localMachine"));
        return name;
    }

    private Region createGraphic(FileSystemStore s) {
        var provider = DataStoreProviders.byStore(s);
        var img = new PrettyImageComp(new SimpleStringProperty(provider.getDisplayIconFileName()), 16, 16);
        return new Label(getName(s), img.createRegion());
    }

    @Override
    protected Region createSimple() {
        var comboBox = new CustomComboBoxBuilder<>(selected, this::createGraphic, null, v -> true);
        comboBox.addFilter((v, s) -> getName(v).toLowerCase().contains(s));
        comboBox.add(new LocalStore());
        XPipeDaemon.getInstance().getNamedStores().stream()
                .filter(e -> e instanceof FileSystemStore)
                .map(e -> (FileSystemStore) e)
                .forEach(comboBox::add);
        ComboBox<Node> cb = comboBox.build();
        cb.getStyleClass().add("choice-comp");
        return cb;
    }
}
