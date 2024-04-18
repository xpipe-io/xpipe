package io.xpipe.app.fxcomps.impl;

import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.util.PlatformThread;

import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;

import lombok.Builder;
import lombok.Value;

import java.util.Objects;

public class TextAreaComp extends Comp<TextAreaComp.Structure> {

    private final Property<String> currentValue;
    private final Property<String> lastAppliedValue;
    private final boolean lazy;

    public TextAreaComp(Property<String> value) {
        this(value, false);
    }

    public TextAreaComp(Property<String> value, boolean lazy) {
        this.lastAppliedValue = value;
        this.currentValue = new SimpleStringProperty(value.getValue());
        this.lazy = lazy;
        value.subscribe(val -> {
            this.currentValue.setValue(val);
        });
    }

    @Override
    public Structure createBase() {
        var text = new TextArea(currentValue.getValue() != null ? currentValue.getValue() : null);
        text.setPrefRowCount(5);
        text.textProperty().addListener((c, o, n) -> {
            currentValue.setValue(n != null && n.length() > 0 ? n : null);
        });
        lastAppliedValue.addListener((c, o, n) -> {
            currentValue.setValue(n);
            PlatformThread.runLaterIfNeeded(() -> {
                // Check if control value is the same. Then don't set it as that might cause bugs
                if (Objects.equals(text.getText(), n)
                        || (n == null && text.getText().isEmpty())) {
                    return;
                }

                text.setText(n);
            });
        });

        text.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                lastAppliedValue.setValue(currentValue.getValue());
            }
        });

        var anchorPane = new AnchorPane(text);
        AnchorPane.setBottomAnchor(text, 0.0);
        AnchorPane.setTopAnchor(text, 0.0);
        AnchorPane.setLeftAnchor(text, 0.0);
        AnchorPane.setRightAnchor(text, 0.0);

        if (lazy) {
            var isEqual = Bindings.createBooleanBinding(
                    () -> Objects.equals(lastAppliedValue.getValue(), currentValue.getValue()),
                    currentValue,
                    lastAppliedValue);
            var button = new IconButtonComp("mdi2c-checkbox-marked-outline")
                    .hide(isEqual)
                    .createRegion();
            anchorPane.getChildren().add(button);
            AnchorPane.setBottomAnchor(button, 10.0);
            AnchorPane.setRightAnchor(button, 10.0);

            text.prefWidthProperty().bind(anchorPane.widthProperty());
            text.prefHeightProperty().bind(anchorPane.heightProperty());
        }

        return new Structure(anchorPane, text);
    }

    @Value
    @Builder
    public static class Structure implements CompStructure<AnchorPane> {
        AnchorPane pane;
        TextArea textArea;

        @Override
        public AnchorPane get() {
            return pane;
        }
    }
}
