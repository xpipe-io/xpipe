package io.xpipe.app.comp.base;

import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.fxcomps.util.SimpleChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

import java.util.Map;

public class MultiContentComp extends SimpleComp {

    private final Map<Comp<?>, ObservableValue<Boolean>> content;

    public MultiContentComp(Map<Comp<?>, ObservableValue<Boolean>> content) {
        this.content = content;
    }

    @Override
    protected Region createSimple() {
        var stack = new StackPane();
        stack.setPickOnBounds(false);
        for (Map.Entry<Comp<?>, ObservableValue<Boolean>> entry : content.entrySet()) {
            var region = entry.getKey().createRegion();
            stack.getChildren().add(region);
            SimpleChangeListener.apply(PlatformThread.sync(entry.getValue()), val -> {
                region.setManaged(val);
                region.setVisible(val);
            });
        }
        return stack;
    }
}
