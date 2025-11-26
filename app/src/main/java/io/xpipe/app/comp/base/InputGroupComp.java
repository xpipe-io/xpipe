package io.xpipe.app.comp.base;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.CompStructure;
import io.xpipe.app.comp.SimpleCompStructure;

import javafx.geometry.Pos;
import javafx.scene.layout.Region;

import atlantafx.base.layout.InputGroup;
import lombok.Setter;

import java.util.List;

public class InputGroupComp extends Comp<CompStructure<InputGroup>> {

    private final List<Comp<?>> entries;

    @Setter
    private Comp<?> mainReference;

    public InputGroupComp(List<Comp<?>> comps) {
        entries = List.copyOf(comps);
    }

    @Override
    public CompStructure<InputGroup> createBase() {
        InputGroup b = new InputGroup();
        b.getStyleClass().add("input-group-comp");
        for (var entry : entries) {
            b.getChildren().add(entry.createRegion());
        }
        b.setAlignment(Pos.CENTER);

        if (mainReference != null && entries.contains(mainReference)) {
            var refIndex = entries.indexOf(mainReference);
            var ref = b.getChildren().get(refIndex);
            if (ref instanceof Region refR) {
                for (int i = 0; i < entries.size(); i++) {
                    if (i == refIndex) {
                        continue;
                    }

                    var entry = b.getChildren().get(i);
                    if (!(entry instanceof Region entryR)) {
                        continue;
                    }

                    entryR.minHeightProperty().bind(refR.heightProperty());
                    entryR.maxHeightProperty().bind(refR.heightProperty());
                    entryR.prefHeightProperty().bind(refR.heightProperty());
                }
            }

            b.focusedProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue) {
                    ref.requestFocus();
                }
            });
        }

        return new SimpleCompStructure<>(b);
    }
}
