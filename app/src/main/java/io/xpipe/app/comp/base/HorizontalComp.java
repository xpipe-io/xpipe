package io.xpipe.app.comp.base;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.CompStructure;
import io.xpipe.app.comp.SimpleCompStructure;

import io.xpipe.app.util.PlatformThread;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;

public class HorizontalComp extends Comp<CompStructure<HBox>> {

    private final ObservableList<Comp<?>> entries;

    public HorizontalComp(List<Comp<?>> comps) {
        entries = FXCollections.observableArrayList(List.copyOf(comps));
    }

    public HorizontalComp(ObservableList<Comp<?>> entries) {
        this.entries = PlatformThread.sync(entries);
    }

    public Comp<CompStructure<HBox>> spacing(double spacing) {
        return apply(struc -> struc.get().setSpacing(spacing));
    }

    @Override
    public CompStructure<HBox> createBase() {
        var b = new HBox();
        b.getStyleClass().add("horizontal-comp");
        entries.addListener((ListChangeListener<? super Comp<?>>) c -> {
            b.getChildren().setAll(c.getList().stream().map(Comp::createRegion).toList());
        });
        for (var entry : entries) {
            b.getChildren().add(entry.createRegion());
        }
        b.setAlignment(Pos.CENTER);
        return new SimpleCompStructure<>(b);
    }
}
