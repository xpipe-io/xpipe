package io.xpipe.app.hub.comp;

import io.xpipe.app.comp.SimpleRegionBuilder;
import io.xpipe.app.platform.PlatformThread;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.layout.Region;

import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.function.Predicate;

public class DataStoreCategoryChoiceComp extends SimpleRegionBuilder {

    private final StoreCategoryWrapper root;
    private final Property<StoreCategoryWrapper> external;
    private final Property<StoreCategoryWrapper> value;
    private final boolean applyExternalInitially;
    private final Predicate<StoreCategoryWrapper> filter;

    public DataStoreCategoryChoiceComp(
            StoreCategoryWrapper root,
            Property<StoreCategoryWrapper> external,
            Property<StoreCategoryWrapper> value,
            boolean applyExternalInitially, Predicate<StoreCategoryWrapper> filter
    ) {
        this.root = root;
        this.external = external;
        this.value = value;
        this.applyExternalInitially = applyExternalInitially;
        this.filter = filter;
    }

    @Override
    protected Region createSimple() {
        var initialized = new SimpleBooleanProperty();
        var last = value.getValue();
        external.subscribe(newValue -> {
            if (newValue == null) {
                value.setValue(root);
            } else if (root == null) {
                value.setValue(newValue);
            } else if (!newValue.getRoot().equals(root)) {
                if (!initialized.get()) {
                    value.setValue(root);
                }
            } else {
                value.setValue(newValue);
            }
            initialized.set(true);
        });
        if (!applyExternalInitially) {
            value.setValue(last);
        }
        var box = new ComboBox<>(StoreViewState.get().getSortedCategories(root).filtered(filter).getList());
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
                textProperty().bind(w.getShownName());
                setPadding(new Insets(6, 6, 6, 8 + (indent ? w.getDepth() * 8 : 0)));
            } else {
                setText("None");
            }
        }
    }
}
