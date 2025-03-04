package io.xpipe.app.comp.base;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.CompStructure;
import io.xpipe.app.comp.SimpleCompStructure;
import io.xpipe.app.util.PlatformThread;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.HBox;

import java.util.List;

public class ToolbarComp extends Comp<CompStructure<ToolBar>> {

    private final ObservableList<Comp<?>> entries;

    public ToolbarComp(List<Comp<?>> comps) {
        entries = FXCollections.observableArrayList(List.copyOf(comps));
    }

    public ToolbarComp(ObservableList<Comp<?>> entries) {
        this.entries = PlatformThread.sync(entries);
    }

    @Override
    public CompStructure<ToolBar> createBase() {
        var b = new ToolBar();
        b.getStyleClass().add("horizontal-comp");
        entries.addListener((ListChangeListener<? super Comp<?>>) c -> {
            b.getItems().setAll(c.getList().stream().map(Comp::createRegion).toList());
        });
        for (var entry : entries) {
            b.getItems().add(entry.createRegion());
        }
        return new SimpleCompStructure<>(b);
    }
}
