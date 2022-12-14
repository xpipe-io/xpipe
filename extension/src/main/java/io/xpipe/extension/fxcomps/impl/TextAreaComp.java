package io.xpipe.extension.fxcomps.impl;

import io.xpipe.extension.fxcomps.SimpleComp;
import io.xpipe.extension.fxcomps.util.PlatformThread;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;

import java.util.Objects;

public class TextAreaComp extends SimpleComp {

    private final Property<String> value;
    private final Property<String> lazyValue = new SimpleStringProperty();
    private final boolean lazy;

    public TextAreaComp(Property<String> value) {
        this(value, false);
    }

    public TextAreaComp(Property<String> value, boolean lazy) {
        this.value = value;
        this.lazy = lazy;
        if (!lazy) {
            value.bind(lazyValue);
        }
    }

    @Override
    protected Region createSimple() {
        var text = new TextArea(value.getValue() != null ? value.getValue() : null);
        text.textProperty().addListener((c, o, n) -> {
            lazyValue.setValue(n != null && n.length() > 0 ? n : null);
        });
        value.addListener((c, o, n) -> {
            lazyValue.setValue(n);
            PlatformThread.runLaterIfNeeded(() -> {
                text.setText(n);
            });
        });

        text.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                value.setValue(lazyValue.getValue());
            }
        });

        if (lazy) {
            var isEqual = Bindings.createBooleanBinding(() -> Objects.equals(lazyValue.getValue(), value.getValue()), value, lazyValue);
            var button = new IconButtonComp("mdi2c-checkbox-marked-outline").hide(isEqual).createRegion();
            var anchorPane = new AnchorPane(text, button);
            AnchorPane.setBottomAnchor(button, 5.0 );
            AnchorPane.setRightAnchor(button, 5.0);

            text.prefWidthProperty().bind(anchorPane.widthProperty());

            return anchorPane;
        }

        return text;
    }
}
