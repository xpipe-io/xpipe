package io.xpipe.app.comp.base;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.util.PlatformThread;

import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

import java.util.Map;

public class MultiContentComp extends SimpleComp {

    private final Map<Comp<?>, ObservableValue<Boolean>> content;

    public MultiContentComp(Map<Comp<?>, ObservableValue<Boolean>> content) {
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

        for (Map.Entry<Comp<?>, ObservableValue<Boolean>> e : content.entrySet()) {
            var r = e.getKey().createRegion();
            PlatformThread.runNestedLoopIteration();
            e.getValue().subscribe(val -> {
                PlatformThread.runLaterIfNeeded(() -> {
                    r.setManaged(val);
                    r.setVisible(val);
                });
            });
            m.put(e.getKey(), r);
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
