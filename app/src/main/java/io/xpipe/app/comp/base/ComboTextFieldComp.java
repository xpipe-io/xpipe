package io.xpipe.app.comp.base;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.CompStructure;
import io.xpipe.app.comp.SimpleCompStructure;
import io.xpipe.app.util.PlatformThread;

import javafx.beans.property.Property;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.util.Callback;

import java.util.List;
import java.util.Objects;

public class ComboTextFieldComp extends Comp<CompStructure<ComboBox<String>>> {

    private final Property<String> value;
    private final List<String> predefinedValues;
    private final Callback<ListView<String>, ListCell<String>> customCellFactory;

    public ComboTextFieldComp(
            Property<String> value,
            List<String> predefinedValues,
            Callback<ListView<String>, ListCell<String>> customCellFactory) {
        this.value = value;
        this.predefinedValues = predefinedValues;
        this.customCellFactory = customCellFactory;
    }

    @Override
    public CompStructure<ComboBox<String>> createBase() {
        var text = new ComboBox<>(FXCollections.observableList(predefinedValues));
        text.setEditable(true);
        text.setMaxWidth(2000);
        text.setValue(value.getValue() != null ? value.getValue() : null);
        text.valueProperty().addListener((c, o, n) -> {
            value.setValue(n != null && n.length() > 0 ? n : null);
        });
        value.addListener((c, o, n) -> {
            PlatformThread.runLaterIfNeeded(() -> {
                // Check if control value is the same. Then don't set it as that might cause bugs
                if (Objects.equals(text.getValue(), n)
                        || (n == null && text.getValue().isEmpty())) {
                    return;
                }

                text.setValue(n);
            });
        });

        if (customCellFactory != null) {
            text.setCellFactory(customCellFactory);
        }

        text.setOnKeyPressed(ke -> {
            if (ke.getCode().equals(KeyCode.ENTER)) {
                text.getScene().getRoot().requestFocus();
            }
            ke.consume();
        });

        return new SimpleCompStructure<>(text);
    }
}
