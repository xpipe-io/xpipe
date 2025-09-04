package io.xpipe.app.comp.base;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.CompStructure;
import io.xpipe.app.comp.SimpleCompStructure;
import io.xpipe.app.platform.PlatformThread;

import javafx.beans.property.Property;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;

import atlantafx.base.theme.Styles;

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
        range.subscribe(val -> {
            PlatformThread.runLaterIfNeeded(() -> {
                if (!val.containsKey(value.getValue())) {
                    this.value.setValue(null);
                }

                box.getChildren().clear();
                for (var entry : val.entrySet()) {
                    var b = new ToggleButton(entry.getValue().getValue());
                    b.setOnAction(e -> {
                        if (entry.getKey().equals(value.getValue())) {
                            value.setValue(null);
                        } else {
                            value.setValue(entry.getKey());
                        }
                        e.consume();
                    });
                    group.getToggles().add(b);
                    box.getChildren().add(b);
                    if (entry.getKey().equals(value.getValue())) {
                        b.setSelected(true);
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
        });

        return new SimpleCompStructure<>(box);
    }
}
