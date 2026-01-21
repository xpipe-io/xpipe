package io.xpipe.app.comp.base;



import io.xpipe.app.comp.SimpleRegionBuilder;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.platform.PlatformThread;

import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import org.int4.fx.builders.common.AbstractRegionBuilder;
import io.xpipe.app.comp.BaseRegionBuilder;

import java.util.Map;

public class MultiContentComp extends SimpleRegionBuilder {

    private final boolean requestFocus;
    private final boolean log;
    private final Map<BaseRegionBuilder<?,?>, ObservableValue<Boolean>> content;

    public MultiContentComp(boolean requestFocus, Map<BaseRegionBuilder<?,?>, ObservableValue<Boolean>> content, boolean log) {
        this.requestFocus = requestFocus;
        this.log = log;
        this.content = FXCollections.observableMap(content);
    }

    @Override
    protected Region createSimple() {
        ObservableMap<BaseRegionBuilder<?,?>, Region> m = FXCollections.observableHashMap();
        var stack = new StackPane();
        m.addListener((MapChangeListener<? super BaseRegionBuilder<?,?>, Region>) change -> {
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

        for (Map.Entry<BaseRegionBuilder<?,?>, ObservableValue<Boolean>> e : content.entrySet()) {
            var name = e.getKey().getClass().getSimpleName();
            if (log) {
                TrackEvent.trace("Creating content tab region for element " + name);
            }
            var r = e.getKey().build();
            if (log) {
                TrackEvent.trace("Created content tab region for element " + name);
            }
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
            if (log) {
                TrackEvent.trace("Added content tab region for element " + name);
            }
        }

        return stack;
    }

    //    Lazy impl
    //    @Override
    //    protected Region createSimple() {
    //        var stack = new StackPane();
    //        for (Map.Entry<BaseRegionBuilder<?,?>, ObservableValue<Boolean>> e : content.entrySet()) {
    //            var r = e.getKey().build();
    //            e.getValue().subscribe(val -> {
    //                PlatformThread.runLaterIfNeeded(() -> {
    //                    r.setManaged(val);
    //                    r.setVisible(val);
    //                    if (val && !stack.getChildren().contains(r)) {
    //                        stack.getChildren().add(r);
    //                    } else {
    //                        stack.getChildren().remove(r);
    //                    }
    //                });
    //            });
    //        }
    //
    //        return stack;
    //    }
}
