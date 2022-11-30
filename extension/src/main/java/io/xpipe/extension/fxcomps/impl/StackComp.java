package io.xpipe.extension.fxcomps.impl;

import io.xpipe.extension.fxcomps.Comp;
import io.xpipe.extension.fxcomps.CompStructure;
import io.xpipe.extension.fxcomps.SimpleCompStructure;
import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;

import java.util.List;

public class StackComp extends Comp<CompStructure<StackPane>> {

    private final List<Comp<?>> comps;

    public StackComp(List<Comp<?>> comps) {
        this.comps = List.copyOf(comps);
    }

    @Override
    public CompStructure<StackPane> createBase() {
        var pane = new StackPane();
        for (var c : comps) {
            pane.getChildren().add(c.createRegion());
        }
        pane.setAlignment(Pos.CENTER);
        pane.setPickOnBounds(false);
        return new SimpleCompStructure<>(pane);
    }
}
