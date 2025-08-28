package io.xpipe.app.comp.base;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.CompStructure;
import io.xpipe.app.comp.SimpleCompStructure;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.platform.PlatformThread;

import javafx.beans.property.Property;
import javafx.beans.value.ObservableValue;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import atlantafx.base.controls.ToggleSwitch;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = true)
public class ToggleSwitchComp extends Comp<CompStructure<ToggleSwitch>> {

    Property<Boolean> selected;
    ObservableValue<String> name;
    ObservableValue<LabelGraphic> graphic;

    @Override
    public CompStructure<ToggleSwitch> createBase() {
        var s = new ToggleSwitch();
        s.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.SPACE || event.getCode() == KeyCode.ENTER) {
                s.setSelected(!s.isSelected());
                event.consume();
            }
        });
        s.setAlignment(Pos.CENTER);
        s.getStyleClass().add("toggle-switch-comp");
        s.setSelected(selected.getValue() != null ? selected.getValue() : false);
        s.selectedProperty().addListener((observable, oldValue, newValue) -> {
            selected.setValue(newValue);
        });
        selected.addListener((observable, oldValue, newValue) -> {
            PlatformThread.runLaterIfNeeded(() -> {
                s.setSelected(newValue);
            });
        });
        if (name != null) {
            name.subscribe(value -> {
                PlatformThread.runLaterIfNeeded(() -> {
                    s.setText(value);
                });
            });
        }
        if (graphic != null) {
            graphic.subscribe(value -> {
                PlatformThread.runLaterIfNeeded(() -> {
                    s.setGraphic(value.createGraphicNode());
                });
            });
            s.pseudoClassStateChanged(PseudoClass.getPseudoClass("has-graphic"), true);
        }
        return new SimpleCompStructure<>(s);
    }
}
