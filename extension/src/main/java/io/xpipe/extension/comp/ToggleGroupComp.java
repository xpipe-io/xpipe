package io.xpipe.extension.comp;

import io.xpipe.fxcomps.Comp;
import io.xpipe.fxcomps.CompStructure;
import io.xpipe.fxcomps.util.PlatformUtil;
import javafx.beans.property.Property;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import org.apache.commons.collections4.BidiMap;

import java.util.function.Supplier;

public class ToggleGroupComp<T> extends Comp<CompStructure<HBox>> {

    private final Property<T> value;
    private final BidiMap<T, Supplier<String>> range;

    public ToggleGroupComp(Property<T> value, BidiMap<T, Supplier<String>> range) {
        this.value = value;
        this.range = range;
    }

    public BidiMap<T, Supplier<String>> getRange() {
        return range;
    }

    @Override
    public CompStructure<HBox> createBase() {
        var box = new HBox();
        box.getStyleClass().add("toggle-group-comp");
        ToggleGroup group = new ToggleGroup();
        for (var entry : range.entrySet()) {
            var b = new ToggleButton(entry.getValue().get());
            b.setOnAction(e -> {
                value.setValue(entry.getKey());
                e.consume();
            });
            box.getChildren().add(b);
            b.setToggleGroup(group);
            value.addListener((c, o, n) -> {
                PlatformUtil.runLaterIfNeeded(() -> b.setSelected(entry.equals(n)));
            });
            if (entry.getKey().equals(value.getValue())) {
                b.setSelected(true);
            }
        }
        box.getChildren().get(0).getStyleClass().add("first");
        box.getChildren().get(box.getChildren().size() - 1).getStyleClass().add("last");

        group.selectedToggleProperty().addListener((obsVal, oldVal, newVal) -> {
            if (newVal == null)
                oldVal.setSelected(true);
        });

        return new CompStructure<>(box);
    }
}
