package io.xpipe.app.util;

import io.xpipe.app.comp.store.StoreCategoryWrapper;
import io.xpipe.app.comp.store.StoreViewState;
import io.xpipe.app.comp.SimpleComp;

import javafx.beans.property.Property;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.layout.Region;

import lombok.EqualsAndHashCode;
import lombok.Value;

public class DataStoreCategoryChoiceComp extends SimpleComp {

    private final StoreCategoryWrapper root;
    private final Property<StoreCategoryWrapper> external;
    private final Property<StoreCategoryWrapper> value;

    public DataStoreCategoryChoiceComp(
            StoreCategoryWrapper root, Property<StoreCategoryWrapper> external, Property<StoreCategoryWrapper> value) {
        this.root = root;
        this.external = external;
        this.value = value;
    }

    @Override
    protected Region createSimple() {
        external.subscribe(newValue -> {
            if (newValue == null) {
                value.setValue(root);
            } else if (root == null) {
                value.setValue(newValue);
            } else if (!newValue.getRoot().equals(root)) {
                value.setValue(root);
            } else {
                value.setValue(newValue);
            }
        });
        var box = new ComboBox<>(StoreViewState.get().getSortedCategories(root).getList());
        box.setValue(value.getValue());
        box.valueProperty().addListener((observable, oldValue, newValue) -> {
            value.setValue(newValue);
        });
        value.addListener((observable, oldValue, newValue) -> {
            PlatformThread.runLaterIfNeeded(() -> box.setValue(newValue));
        });
        box.setCellFactory(param -> {
            return new Cell(true);
        });
        box.setButtonCell(new Cell(false));
        return box;
    }

    @EqualsAndHashCode(callSuper = true)
    @Value
    private static class Cell extends ListCell<StoreCategoryWrapper> {

        boolean indent;

        @Override
        protected void updateItem(StoreCategoryWrapper w, boolean empty) {
            super.updateItem(w, empty);
            textProperty().unbind();
            if (w != null) {
                textProperty().bind(w.nameProperty());
                setPadding(new Insets(6, 6, 6, 8 + (indent ? w.getDepth() * 8 : 0)));
            } else {
                setText("None");
            }
        }
    }
}
