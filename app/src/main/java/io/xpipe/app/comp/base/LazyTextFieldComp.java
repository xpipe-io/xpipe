package io.xpipe.app.comp.base;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.CompStructure;
import io.xpipe.app.platform.PlatformThread;
import io.xpipe.core.OsType;

import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;

import lombok.Builder;
import lombok.Value;

import java.util.Objects;

public class LazyTextFieldComp extends Comp<LazyTextFieldComp.Structure> {

    private final Property<String> currentValue;
    private final Property<String> appliedValue;

    public LazyTextFieldComp(Property<String> appliedValue) {
        this.appliedValue = appliedValue;
        this.currentValue = new SimpleStringProperty(appliedValue.getValue());
    }

    @Override
    public Structure createBase() {
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
            if (n && OsType.getLocal() != OsType.WINDOWS) {
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

        var sizeLabel = new Label();
        sizeLabel.maxWidthProperty().bind(sizeLabel.prefWidthProperty());
        sizeLabel.textProperty().bind(currentValue);
        sizeLabel.setVisible(false);
        sizeLabel.paddingProperty().bind(r.paddingProperty());

        var stack = new StackPane();
        stack.getChildren().addAll(sizeLabel, r);
        stack.setAlignment(Pos.CENTER_LEFT);
        stack.prefWidthProperty().bind(sizeLabel.prefWidthProperty());
        stack.prefHeightProperty().bind(r.heightProperty());

        stack.focusedProperty().addListener((observable, oldValue, n) -> {
            if (n) {
                r.setDisable(false);
                r.requestFocus();
            }
        });

        return new Structure(stack, r);
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
