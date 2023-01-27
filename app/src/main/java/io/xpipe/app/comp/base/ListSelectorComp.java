package io.xpipe.app.comp.base;

import com.jfoenix.controls.JFXCheckBox;
import io.xpipe.extension.fxcomps.SimpleComp;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
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
    ListProperty<T> selected = new SimpleListProperty<>(FXCollections.observableArrayList());

    @Override
    protected Region createSimple() {
        var vbox = new VBox();
        for (var v : values) {
            var cb = new JFXCheckBox(null);
            cb.selectedProperty().addListener((c, o, n) -> {
                if (n) {
                    selected.add(v);
                } else {
                    selected.remove(v);
                }
            });
            cb.setSelected(true);
            var l = new Label(toString.apply(v), cb);
            vbox.getChildren().add(l);
        }
        var sp = new ScrollPane(vbox);
        sp.setFitToWidth(true);
        return sp;
    }
}
