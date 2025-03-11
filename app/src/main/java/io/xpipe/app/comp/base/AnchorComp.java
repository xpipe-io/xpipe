package io.xpipe.app.comp.base;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.CompStructure;
import io.xpipe.app.comp.SimpleCompStructure;

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
        return new SimpleCompStructure<>(pane);
    }
}
