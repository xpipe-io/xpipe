package io.xpipe.app.fxcomps.impl;

import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.fxcomps.util.SimpleChangeListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;

import java.util.Objects;

public class TextAreaComp extends SimpleComp {

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
        SimpleChangeListener.apply(value, val -> {
            this.currentValue.setValue(val);
        });
    }

    @Override
    protected Region createSimple() {
        var text = new TextArea(currentValue.getValue() != null ? currentValue.getValue() : null);
        text.setPrefRowCount(5);
        text.textProperty().addListener((c, o, n) -> {
            currentValue.setValue(n != null && n.length() > 0 ? n : null);
        });
        lastAppliedValue.addListener((c, o, n) -> {
            currentValue.setValue(n);
            PlatformThread.runLaterIfNeeded(() -> {
                text.setText(n);
            });
        });

        text.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                lastAppliedValue.setValue(currentValue.getValue());
            }
        });

        if (lazy) {
            var isEqual = Bindings.createBooleanBinding(
                    () -> Objects.equals(lastAppliedValue.getValue(), currentValue.getValue()),
                    currentValue,
                    lastAppliedValue);
            var button = new IconButtonComp("mdi2c-checkbox-marked-outline")
                    .hide(isEqual)
                    .createRegion();
            var anchorPane = new AnchorPane(text, button);
            AnchorPane.setBottomAnchor(button, 10.0);
            AnchorPane.setRightAnchor(button, 10.0);

            text.prefWidthProperty().bind(anchorPane.widthProperty());
            text.prefHeightProperty().bind(anchorPane.heightProperty());

            return anchorPane;
        }

        return text;
    }
}
