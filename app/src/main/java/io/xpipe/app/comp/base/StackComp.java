package io.xpipe.app.comp.base;

import io.xpipe.app.comp.RegionBuilder;

import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;

import org.int4.fx.builders.common.AbstractRegionBuilder;

import java.util.List;

public class StackComp extends RegionBuilder<StackPane> {

    private final List<AbstractRegionBuilder<?, ?>> comps;

    public StackComp(List<AbstractRegionBuilder<?, ?>> comps) {
        this.comps = List.copyOf(comps);
    }

    @Override
    public StackPane createSimple() {
        var pane = new StackPane();
        for (var c : comps) {
            pane.getChildren().add(c.build());
        }
        pane.setAlignment(Pos.CENTER);
        return pane;
    }
}
