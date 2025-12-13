package io.xpipe.app.comp.base;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.platform.PlatformThread;

import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

import java.util.Map;

public class MultiContentComp extends SimpleComp {

    private final boolean requestFocus;
    private final boolean log;
    private final Map<Comp<?>, ObservableValue<Boolean>> content;

    public MultiContentComp(boolean requestFocus, Map<Comp<?>, ObservableValue<Boolean>> content, boolean log) {
        this.requestFocus = requestFocus;
        this.log = log;
        this.content = FXCollections.observableMap(content);
    }

    @Override
    protected Region createSimple() {
        ObservableMap<Comp<?>, Region> m = FXCollections.observableHashMap();
        var stack = new StackPane();
        m.addListener((MapChangeListener<? super Comp<?>, Region>) change -> {
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

        for (Map.Entry<Comp<?>, ObservableValue<Boolean>> e : content.entrySet()) {
            var name = e.getKey().getClass().getSimpleName();
            if (log) {
                TrackEvent.trace("Creating content tab region for element " + name);
            }
            var r = e.getKey().createRegion();
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
    //        for (Map.Entry<Comp<?>, ObservableValue<Boolean>> e : content.entrySet()) {
    //            var r = e.getKey().createRegion();
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
