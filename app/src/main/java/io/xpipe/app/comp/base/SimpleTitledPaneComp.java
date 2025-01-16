package io.xpipe.app.comp.base;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.CompStructure;
import io.xpipe.app.comp.SimpleCompStructure;

import javafx.beans.value.ObservableValue;
import javafx.scene.control.TitledPane;

public class SimpleTitledPaneComp extends Comp<CompStructure<TitledPane>> {

    private final ObservableValue<String> name;
    private final Comp<?> content;
    private final boolean collapsible;

    public SimpleTitledPaneComp(ObservableValue<String> name, Comp<?> content, boolean collapsible) {
        this.name = name;
        this.content = content;
        this.collapsible = collapsible;
    }

    @Override
    public CompStructure<TitledPane> createBase() {
        var r = content.createRegion();
        r.getStyleClass().add("content");
        var tp = new TitledPane(null, r);
        tp.textProperty().bind(name);
        tp.getStyleClass().add("simple-titled-pane-comp");
        tp.setExpanded(true);
        tp.setCollapsible(collapsible);
        return new SimpleCompStructure<>(tp);
    }
}
