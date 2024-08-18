package io.xpipe.app.comp.base;

import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.util.PlatformThread;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
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
        var stack = new StackPane();
        for (Map.Entry<Comp<?>, ObservableValue<Boolean>> e : content.entrySet()) {
            var r = e.getKey().createRegion();
            e.getValue().subscribe(val -> {
                PlatformThread.runLaterIfNeeded(() -> {
                    r.setManaged(val);
                    r.setVisible(val);
                    if (val && !stack.getChildren().contains(r)) {
                        stack.getChildren().add(r);
                    } else {
                        stack.getChildren().remove(r);
                    }
                });
            });
        }

        return stack;
    }
}
