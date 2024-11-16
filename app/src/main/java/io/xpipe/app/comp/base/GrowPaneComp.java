package io.xpipe.app.comp.base;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.CompStructure;
import io.xpipe.app.comp.SimpleCompStructure;

import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;

import java.util.List;

public class GrowPaneComp extends Comp<CompStructure<Pane>> {

    private final List<Comp<?>> comps;

    public GrowPaneComp(List<Comp<?>> comps) {
        this.comps = List.copyOf(comps);
    }

    @Override
    public CompStructure<Pane> createBase() {
        var pane = new BorderPane();
        for (var c : comps) {
            pane.setCenter(c.createRegion());
        }
        pane.setPickOnBounds(false);
        return new SimpleCompStructure<>(pane);
    }
}
