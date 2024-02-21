package io.xpipe.app.fxcomps.impl;

import atlantafx.base.theme.Styles;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.SimpleCompStructure;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.fxcomps.util.SimpleChangeListener;
import javafx.beans.property.Property;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;

import java.util.Map;

public class ToggleGroupComp<T> extends Comp<CompStructure<HBox>> {

    private final Property<T> value;
    private final ObservableValue<Map<T, ObservableValue<String>>> range;

    public ToggleGroupComp(Property<T> value, ObservableValue<Map<T, ObservableValue<String>>> range) {
        this.value = value;
        this.range = range;
    }

    @Override
    public CompStructure<HBox> createBase() {
        var box = new HBox();
        box.getStyleClass().add("toggle-group-comp");
        ToggleGroup group = new ToggleGroup();
        SimpleChangeListener.apply(PlatformThread.sync(range), val -> {
            if (!val.containsKey(value.getValue())) {
                this.value.setValue(null);
            }

            box.getChildren().clear();
            for (var entry : val.entrySet()) {
                var b = new ToggleButton(entry.getValue().getValue());
                b.setOnAction(e -> {
                    value.setValue(entry.getKey());
                    e.consume();
                });
                box.getChildren().add(b);
                b.setToggleGroup(group);
                value.addListener((c, o, n) -> {
                    PlatformThread.runLaterIfNeeded(() -> {
                        if (entry.getKey().equals(n)) {
                            group.selectToggle(b);
                        }
                    });
                });
                if (entry.getKey().equals(value.getValue())) {
                    group.selectToggle(b);
                }
            }

            if (box.getChildren().size() > 0) {
                box.getChildren().getFirst().getStyleClass().add(Styles.LEFT_PILL);
                for (int i = 1; i < box.getChildren().size() - 1; i++) {
                    box.getChildren().get(i).getStyleClass().add(Styles.CENTER_PILL);
                }
                box.getChildren().getLast().getStyleClass().add(Styles.RIGHT_PILL);
            }
        });

        group.selectedToggleProperty().addListener((obsVal, oldVal, newVal) -> {
            if (newVal == null) {
                oldVal.setSelected(true);
            }
        });

        return new SimpleCompStructure<>(box);
    }
}
