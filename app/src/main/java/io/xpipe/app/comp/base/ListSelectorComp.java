package io.xpipe.app.comp.base;

import io.xpipe.app.fxcomps.SimpleComp;
import javafx.beans.property.ListProperty;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.List;
import java.util.function.Function;

@Value
@EqualsAndHashCode(callSuper = true)
public class ListSelectorComp<T> extends SimpleComp {

    List<T> values;
    Function<T, String> toString;
    ListProperty<T> selected;

    @Override
    protected Region createSimple() {
        var vbox = new VBox();
        vbox.setSpacing(8);
        vbox.getStyleClass().add("content");
        for (var v : values) {
            var cb = new CheckBox(null);
            cb.selectedProperty().addListener((c, o, n) -> {
                if (n) {
                    selected.add(v);
                } else {
                    selected.remove(v);
                }
            });
            cb.setSelected(selected.contains(v));
            var l = new Label(toString.apply(v), cb);
            vbox.getChildren().add(l);
        }
        var sp = new ScrollPane(vbox);
        sp.setFitToWidth(true);
        return sp;
    }
}
