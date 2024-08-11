package io.xpipe.app.fxcomps.impl;

import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.SimpleCompStructure;

import io.xpipe.app.fxcomps.util.DerivedObservableList;
import io.xpipe.app.fxcomps.util.PlatformThread;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import lombok.AllArgsConstructor;

import java.util.List;

@AllArgsConstructor
public class HorizontalComp extends Comp<CompStructure<HBox>> {

    private final ObservableList<Comp<?>> entries;

    public HorizontalComp(List<Comp<?>> comps) {
        entries = FXCollections.observableList(List.copyOf(comps));
    }

    public Comp<CompStructure<HBox>> spacing(double spacing) {
        return apply(struc -> struc.get().setSpacing(spacing));
    }

    @Override
    public CompStructure<HBox> createBase() {
        HBox b = new HBox();
        b.getStyleClass().add("horizontal-comp");
        var map = new DerivedObservableList<>(entries, false).mapped(comp -> comp.createRegion()).getList();
        b.getChildren().setAll(map);
        map.addListener((ListChangeListener<? super Region>) c -> {
            PlatformThread.runLaterIfNeeded(() -> {
                b.getChildren().setAll(c.getList());
            });
        });
        b.setAlignment(Pos.CENTER);
        return new SimpleCompStructure<>(b);
    }
}
