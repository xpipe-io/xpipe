package io.xpipe.extension.fxcomps.impl;

import io.xpipe.extension.fxcomps.Comp;
import io.xpipe.extension.fxcomps.CompStructure;
import io.xpipe.extension.fxcomps.SimpleCompStructure;
import javafx.scene.layout.Pane;

import java.util.List;

public class PaneComp extends Comp<CompStructure<Pane>> {

    private final List<Comp<?>> comps;

    public PaneComp(List<Comp<?>> comps) {
        this.comps = List.copyOf(comps);
    }

    @Override
    public CompStructure<Pane> createBase() {
        var pane = new Pane();
        for (var c : comps) {
            pane.getChildren().add(c.createRegion());
        }
        pane.setPickOnBounds(false);
        return new SimpleCompStructure<>(pane);
    }
}
