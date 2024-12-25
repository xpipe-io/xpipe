package io.xpipe.app.comp.base;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.CompStructure;
import io.xpipe.app.comp.SimpleCompStructure;

import javafx.geometry.Pos;

import atlantafx.base.layout.InputGroup;

import java.util.List;

public class InputGroupComp extends Comp<CompStructure<InputGroup>> {

    private final List<Comp<?>> entries;

    public InputGroupComp(List<Comp<?>> comps) {
        entries = List.copyOf(comps);
    }

    public Comp<CompStructure<InputGroup>> spacing(double spacing) {
        return apply(struc -> struc.get().setSpacing(spacing));
    }

    @Override
    public CompStructure<InputGroup> createBase() {
        InputGroup b = new InputGroup();
        b.getStyleClass().add("input-group-comp");
        for (var entry : entries) {
            b.getChildren().add(entry.createRegion());
        }
        b.setAlignment(Pos.CENTER);
        return new SimpleCompStructure<>(b);
    }
}
