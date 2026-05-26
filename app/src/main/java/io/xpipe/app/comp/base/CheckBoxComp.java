package io.xpipe.app.comp.base;

import io.xpipe.app.comp.RegionBuilder;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.platform.PlatformThread;

import javafx.beans.property.Property;
import javafx.beans.value.ObservableValue;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;

import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = true)
public class CheckBoxComp extends RegionBuilder<CheckBox> {

    Property<Boolean> selected;
    ObservableValue<String> name;
    ObservableValue<LabelGraphic> graphic;

    @Override
    public CheckBox createSimple() {
        var s = new CheckBox();
        s.setOnAction(event -> event.consume());
        s.setAlignment(Pos.CENTER);
        s.getStyleClass().add("check-box-comp");
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
            s.setAlignment(Pos.CENTER);
            s.pseudoClassStateChanged(PseudoClass.getPseudoClass("has-graphic"), true);
        }
        return s;
    }
}
