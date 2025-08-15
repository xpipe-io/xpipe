package io.xpipe.app.comp.base;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.CompStructure;
import io.xpipe.app.comp.SimpleCompStructure;
import io.xpipe.app.util.PlatformThread;

import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import lombok.Setter;

import java.util.Objects;
import java.util.function.Supplier;

public class ComboTextFieldComp extends Comp<CompStructure<ComboBox<String>>> {

    private final Property<String> value;
    private final ObservableList<String> predefinedValues;
    private final Supplier<ListCell<String>> customCellFactory;

    @Setter
    private ObservableValue<String> prompt;

    public ComboTextFieldComp(
            Property<String> value,
            ObservableList<String> predefinedValues,
            Supplier<ListCell<String>> customCellFactory) {
        this.value = value;
        this.predefinedValues = predefinedValues;
        this.customCellFactory = customCellFactory;
    }

    @Override
    public CompStructure<ComboBox<String>> createBase() {
        var text = new ComboBox<>(predefinedValues);
        text.addEventFilter(KeyEvent.ANY, event -> {
            Platform.runLater(() -> {
                text.commitValue();
            });
        });
        text.setEditable(true);
        text.setMaxWidth(20000);
        text.setValue(value.getValue() != null ? value.getValue() : null);
        text.valueProperty().addListener((c, o, n) -> {
            value.setValue(n != null && n.length() > 0 ? n : null);
        });

        if (prompt != null) {
            prompt.subscribe(filePath -> {
                PlatformThread.runLaterIfNeeded(() -> {
                    text.setPromptText(filePath);
                });
            });
        }

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
            text.setCellFactory(param -> customCellFactory.get());
            text.setButtonCell(customCellFactory.get());
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
