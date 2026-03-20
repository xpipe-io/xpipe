package io.xpipe.app.comp.base;

import io.xpipe.app.comp.BaseRegionBuilder;
import io.xpipe.app.comp.SimpleRegionBuilder;
import io.xpipe.app.platform.PlatformThread;

import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

import java.util.Map;

public class MultiContentComp extends SimpleRegionBuilder {

    private final boolean requestFocus;
    private final Map<BaseRegionBuilder<?, ?>, ObservableValue<Boolean>> content;

    public MultiContentComp(boolean requestFocus, Map<BaseRegionBuilder<?, ?>, ObservableValue<Boolean>> content) {
        this.requestFocus = requestFocus;
        this.content = FXCollections.observableMap(content);
    }

    @Override
    protected Region createSimple() {
        ObservableMap<BaseRegionBuilder<?, ?>, Region> m = FXCollections.observableHashMap();
        var stack = new StackPane();
        m.addListener((MapChangeListener<? super BaseRegionBuilder<?, ?>, Region>) change -> {
            if (change.wasAdded()) {
                stack.getChildren().add(change.getValueAdded());
            } else {
                stack.getChildren().remove(change.getValueRemoved());
            }
        });

        stack.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                var selected = content.entrySet().stream()
                        .filter(e -> e.getValue().getValue())
                        .map(e -> m.get(e.getKey()))
                        .findFirst();
                if (selected.isPresent()) {
                    selected.get().requestFocus();
                }
            }
        });

        for (Map.Entry<BaseRegionBuilder<?, ?>, ObservableValue<Boolean>> e : content.entrySet()) {
            var r = e.getKey().build();
            e.getValue().subscribe(val -> {
                PlatformThread.runLaterIfNeeded(() -> {
                    r.setManaged(val);
                    r.setVisible(val);
                    if (requestFocus && val) {
                        Platform.runLater(() -> {
                            r.requestFocus();
                        });
                    }
                });
            });
            m.put(e.getKey(), r);
        }

        return stack;
    }
}
