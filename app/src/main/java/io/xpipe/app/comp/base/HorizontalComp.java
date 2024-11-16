package io.xpipe.app.comp.base;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.CompStructure;
import io.xpipe.app.comp.SimpleCompStructure;

import javafx.geometry.Pos;
import javafx.scene.layout.HBox;

import java.util.List;

public class HorizontalComp extends Comp<CompStructure<HBox>> {

    private final List<Comp<?>> entries;

    public HorizontalComp(List<Comp<?>> comps) {
        entries = List.copyOf(comps);
    }

    public Comp<CompStructure<HBox>> spacing(double spacing) {
        return apply(struc -> struc.get().setSpacing(spacing));
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
