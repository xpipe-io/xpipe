package io.xpipe.app.fxcomps.impl;

import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.SimpleCompStructure;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;

import java.util.List;

public class HorizontalComp extends Comp<CompStructure<HBox>> {

    private final List<Comp<?>> entries;

    public HorizontalComp(List<Comp<?>> comps) {
        entries = List.copyOf(comps);
    }

    @Override
    public CompStructure<HBox> createBase() {
        HBox b = new HBox();
        b.getStyleClass().add("horizontal-comp");
        for (var entry : entries) {
            b.getChildren().add(entry.createRegion());
        }
        b.setAlignment(Pos.CENTER);
        return new SimpleCompStructure<>(b);
    }
}
