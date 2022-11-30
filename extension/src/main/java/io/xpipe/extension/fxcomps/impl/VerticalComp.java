package io.xpipe.extension.fxcomps.impl;

import io.xpipe.extension.fxcomps.Comp;
import io.xpipe.extension.fxcomps.CompStructure;
import io.xpipe.extension.fxcomps.SimpleCompStructure;
import io.xpipe.extension.fxcomps.util.PlatformThread;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.layout.VBox;

import java.util.List;

public class VerticalComp extends Comp<CompStructure<VBox>> {

    private final ObservableList<Comp<?>> entries;

    public VerticalComp(List<Comp<?>> comps) {
        entries = FXCollections.observableArrayList(List.copyOf(comps));
    }

    public VerticalComp(ObservableList<Comp<?>> entries) {
        this.entries = PlatformThread.sync(entries);
    }

    @Override
    public CompStructure<VBox> createBase() {
        VBox b = new VBox();
        b.getStyleClass().add("vertical-comp");
        entries.addListener((ListChangeListener<? super Comp<?>>) c -> {
            b.getChildren().setAll(c.getList().stream().map(Comp::createRegion).toList());
        });
        for (var entry : entries) {
            b.getChildren().add(entry.createRegion());
        }
        return new SimpleCompStructure<>(b);
    }
}
