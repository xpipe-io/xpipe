package io.xpipe.app.comp.source;

import io.xpipe.app.storage.DataSourceCollection;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.extension.fxcomps.SimpleComp;
import io.xpipe.extension.util.CustomComboBoxBuilder;
import javafx.beans.property.Property;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;

public class DsStorageGroupSelector extends SimpleComp {

    private final Property<DataSourceCollection> selected;

    public DsStorageGroupSelector(Property<DataSourceCollection> selected) {
        this.selected = selected;
    }

    private static Region createGraphic(DataSourceCollection group) {
        if (group == null) {
            return new Label("<>");
        }

        var l = new Label(group.getName());
        return l;
    }

    @Override
    protected ComboBox<Node> createSimple() {
        var comboBox = new CustomComboBoxBuilder<DataSourceCollection>(
                selected, DsStorageGroupSelector::createGraphic, createGraphic(null), v -> true);

        DataStorage.get().getSourceCollections().stream()
                .filter(dataSourceCollection ->
                        !dataSourceCollection.equals(DataStorage.get().getInternalCollection()))
                .forEach(comboBox::add);
        ComboBox<Node> cb = comboBox.build();
        cb.getStyleClass().add("storage-group-selector");
        return cb;
    }
}
