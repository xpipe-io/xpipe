package io.xpipe.app.fxcomps.impl;

import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.SimpleCompStructure;

import javafx.scene.layout.AnchorPane;

import java.util.List;

public class AnchorComp extends Comp<CompStructure<AnchorPane>> {

    private final List<Comp<?>> comps;

    public AnchorComp(List<Comp<?>> comps) {
        this.comps = List.copyOf(comps);
    }

    @Override
    public CompStructure<AnchorPane> createBase() {
        var pane = new AnchorPane();
        for (var c : comps) {
            pane.getChildren().add(c.createRegion());
        }
        pane.setPickOnBounds(false);
        return new SimpleCompStructure<>(pane);
    }
}
