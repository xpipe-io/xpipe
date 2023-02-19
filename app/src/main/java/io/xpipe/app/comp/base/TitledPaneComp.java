package io.xpipe.app.comp.base;

import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.SimpleCompStructure;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TitledPane;

import java.util.concurrent.atomic.AtomicInteger;

public class TitledPaneComp extends Comp<CompStructure<TitledPane>> {

    private final ObservableValue<String> name;
    private final Comp<?> content;
    private final int height;

    public TitledPaneComp(ObservableValue<String> name, Comp<?> content, int height) {
        this.name = name;
        this.content = content;
        this.height = height;
    }

    @Override
    public CompStructure<TitledPane> createBase() {
        var tp = new TitledPane(null, content.createRegion());
        tp.textProperty().bind(name);
        tp.getStyleClass().add("titled-pane-comp");
        tp.setExpanded(false);
        tp.setAnimated(false);
        AtomicInteger minimizedSize = new AtomicInteger();
        tp.expandedProperty().addListener((c, o, n) -> {
            if (n) {
                if (minimizedSize.get() == 0) {
                    minimizedSize.set((int) tp.getHeight());
                }
                tp.setPrefHeight(height);
            } else {
                tp.setPrefHeight(minimizedSize.get());
            }
        });
        return new SimpleCompStructure<>(tp);
    }
}
