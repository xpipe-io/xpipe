package io.xpipe.app.comp.base;

import com.jfoenix.controls.JFXTextField;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.fxcomps.util.SimpleChangeListener;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import lombok.Builder;
import lombok.Value;

public class LazyTextFieldComp extends Comp<LazyTextFieldComp.Structure> {

    private final Property<String> currentValue;
    private final Property<String> appliedValue;

    public LazyTextFieldComp(Property<String> appliedValue) {
        this.appliedValue = appliedValue;
        this.currentValue = new SimpleStringProperty(appliedValue.getValue());
    }

    @Override
    public LazyTextFieldComp.Structure createBase() {
        var sp = new StackPane();
        var r = new JFXTextField();

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
            if (!n) {
                appliedValue.setValue(currentValue.getValue());
            }
        });

        sp.focusedProperty().addListener((c, o, n) -> {
            if (n) {
                r.setDisable(false);
                r.requestFocus();
            }
        });

        // Handles external updates
        PlatformThread.sync(appliedValue).addListener((observable, oldValue, newValue) -> {
            r.setText(newValue);
            currentValue.setValue(newValue);
        });

        r.setPrefWidth(0);
        sp.getChildren().add(r);
        sp.prefWidthProperty().bind(r.prefWidthProperty());
        sp.prefHeightProperty().bind(r.prefHeightProperty());
        r.setDisable(true);

        SimpleChangeListener.apply(currentValue, val -> {
            PlatformThread.runLaterIfNeeded(() -> r.setText(val));
        });
        r.textProperty().addListener((observable, oldValue, newValue) -> {
            currentValue.setValue(newValue);
        });

        r.focusedProperty().addListener((c, o, n) -> {
            if (!n) {
                r.setDisable(true);
            }
        });
        r.getStyleClass().add("lazy-text-field-comp");
        return new Structure(sp, r);
    }

    @Value
    @Builder
    public static class Structure implements CompStructure<StackPane> {
        StackPane pane;
        JFXTextField textField;

        @Override
        public StackPane get() {
            return pane;
        }
    }
}
