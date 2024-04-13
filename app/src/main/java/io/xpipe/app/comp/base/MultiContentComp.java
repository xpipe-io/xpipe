package io.xpipe.app.comp.base;

import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.util.PlatformThread;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

import java.util.Map;

public class MultiContentComp extends SimpleComp {

    private final ObservableMap<Comp<?>, ObservableValue<Boolean>> content;

    public MultiContentComp(Map<Comp<?>, ObservableValue<Boolean>> content) {
        this.content = FXCollections.observableMap(content);
    }

    public MultiContentComp(ObservableMap<Comp<?>, ObservableValue<Boolean>> content) {
        this.content = content;
    }

    @Override
    protected Region createSimple() {
        ObservableMap<Comp<?>, Region> m = FXCollections.observableHashMap();
        content.addListener((MapChangeListener<? super Comp<?>, ? super ObservableValue<Boolean>>) change -> {
            if (change.wasAdded()) {
                var r = change.getKey().createRegion();
                change.getValueAdded().subscribe(val -> {
                    PlatformThread.runLaterIfNeeded(() -> {
                        r.setManaged(val);
                        r.setVisible(val);
                    });
                });
                m.put(change.getKey(), r);
            } else {
                m.remove(change.getKey());
            }
        });

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
}
