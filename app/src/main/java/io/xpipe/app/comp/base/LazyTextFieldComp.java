package io.xpipe.app.comp.base;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.CompStructure;
import io.xpipe.app.comp.SimpleCompStructure;
import io.xpipe.app.util.PlatformThread;

import io.xpipe.core.process.OsType;
import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;

import lombok.Builder;
import lombok.Value;

import java.util.Objects;

public class LazyTextFieldComp extends Comp<CompStructure<TextField>> {

    private final Property<String> currentValue;
    private final Property<String> appliedValue;

    public LazyTextFieldComp(Property<String> appliedValue) {
        this.appliedValue = appliedValue;
        this.currentValue = new SimpleStringProperty(appliedValue.getValue());
    }

    @Override
    public CompStructure<TextField> createBase() {
        var r = new TextField();

        r.setOnKeyPressed(ke -> {
            if (ke.getCode().equals(KeyCode.ESCAPE)) {
                currentValue.setValue(appliedValue.getValue());
            }

            if (ke.getCode().equals(KeyCode.ENTER) || ke.getCode().equals(KeyCode.ESCAPE)) {
                r.getScene().getRoot().requestFocus();
            }

            ke.consume();
        });

        r.focusedProperty().addListener((c, o, n) -> {
            if (n) {
                Platform.runLater(() -> {
                    r.selectEnd();
                });
            }

            if (!n) {
                appliedValue.setValue(currentValue.getValue());
                r.setDisable(true);
            }
        });

        // Handles external updates
        appliedValue.addListener((observable, oldValue, n) -> {
            PlatformThread.runLaterIfNeeded(() -> {
                currentValue.setValue(n);
            });
        });

        r.setMinWidth(0);
        r.setDisable(true);
        r.prefWidthProperty().bind(r.minWidthProperty());

        currentValue.subscribe(n -> {
            PlatformThread.runLaterIfNeeded(() -> {
                // Check if control value is the same. Then don't set it as that might cause bugs
                if (Objects.equals(r.getText(), n) || (n == null && r.getText().isEmpty())) {
                    return;
                }

                r.setText(n);
            });
        });
        r.textProperty().addListener((observable, oldValue, newValue) -> {
            currentValue.setValue(newValue);
        });

        r.getStyleClass().add("lazy-text-field-comp");
        return new SimpleCompStructure<>(r);
    }

    @Value
    @Builder
    public static class Structure implements CompStructure<StackPane> {
        StackPane pane;
        TextField textField;

        @Override
        public StackPane get() {
            return pane;
        }
    }
}
