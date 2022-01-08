package io.xpipe.extension.comp;

import io.xpipe.fxcomps.CompStructure;
import io.xpipe.fxcomps.store.DefaultValueStoreComp;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import org.apache.commons.collections4.BidiMap;

import java.util.function.Supplier;

public class ToggleGroupComp<T> extends DefaultValueStoreComp<CompStructure<HBox>, T> {

    private final BidiMap<T, Supplier<String>> range;

    public ToggleGroupComp(T defaultVal, BidiMap<T, Supplier<String>> range) {
        super(defaultVal);
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
                set(entry.getKey());
                e.consume();
            });
            box.getChildren().add(b);
            b.setToggleGroup(group);
            valueProperty().addListener((c, o, n) -> {
                b.setSelected(entry.equals(n));
            });
            if (entry.getKey().equals(getValue())) {
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
