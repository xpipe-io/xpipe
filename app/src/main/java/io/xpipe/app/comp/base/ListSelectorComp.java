package io.xpipe.app.comp.base;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.fxcomps.SimpleComp;
import javafx.beans.property.ListProperty;
import javafx.geometry.Orientation;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Value
@EqualsAndHashCode(callSuper = true)
public class ListSelectorComp<T> extends SimpleComp {

    List<T> values;
    Function<T, String> toString;
    ListProperty<T> selected;
    boolean showAllSelector;

    @Override
    protected Region createSimple() {
        var vbox = new VBox();
        vbox.setSpacing(8);
        vbox.getStyleClass().add("content");
        var cbs = new ArrayList<CheckBox>();
        for (var v : values) {
            var cb = new CheckBox(null);
            cbs.add(cb);
            cb.setSelected(selected.contains(v));
            cb.selectedProperty().addListener((c, o, n) -> {
                if (n) {
                    selected.add(v);
                } else {
                    selected.remove(v);
                }
            });
            var l = new Label(toString.apply(v), cb);
            l.setGraphicTextGap(9);
            l.setOnMouseClicked(event -> cb.setSelected(!cb.isSelected()));
            vbox.getChildren().add(l);
        }

        if (showAllSelector) {
            var allSelector = new CheckBox(null);
            allSelector.setSelected(values.size() == selected.size());
            allSelector.selectedProperty().addListener((observable, oldValue, newValue) -> {
                cbs.forEach(checkBox -> checkBox.setSelected(newValue));
            });
            var l = new Label(null, allSelector);
            l.textProperty().bind(AppI18n.observable("selectAll"));
            l.setGraphicTextGap(9);
            l.setOnMouseClicked(event -> allSelector.setSelected(!allSelector.isSelected()));
            vbox.getChildren().add(new Separator(Orientation.HORIZONTAL));
            vbox.getChildren().add(l);
        }

        var sp = new ScrollPane(vbox);
        sp.setFitToWidth(true);
        return sp;
    }
}
