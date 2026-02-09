package io.xpipe.app.comp.base;

import io.xpipe.app.comp.BaseRegionBuilder;
import io.xpipe.app.comp.RegionBuilder;

import javafx.scene.layout.AnchorPane;

import java.util.List;

public class AnchorComp extends RegionBuilder<AnchorPane> {

    private final List<BaseRegionBuilder<?, ?>> comps;

    public AnchorComp(List<BaseRegionBuilder<?, ?>> comps) {
        this.comps = List.copyOf(comps);
    }

    @Override
    public AnchorPane createSimple() {
        var pane = new AnchorPane();
        for (var c : comps) {
            pane.getChildren().add(c.build());
        }
        return pane;
    }
}
