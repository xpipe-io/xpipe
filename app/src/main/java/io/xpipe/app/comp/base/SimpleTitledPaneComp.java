package io.xpipe.app.comp.base;

import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.SimpleCompStructure;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TitledPane;

public class SimpleTitledPaneComp extends Comp<CompStructure<TitledPane>> {

    private final ObservableValue<String> name;
    private final Comp<?> content;

    public SimpleTitledPaneComp(ObservableValue<String> name, Comp<?> content) {
        this.name = name;
        this.content = content;
    }

    @Override
    public CompStructure<TitledPane> createBase() {
        var tp = new TitledPane(null, content.createRegion());
        tp.textProperty().bind(name);
        tp.getStyleClass().add("simple-titled-pane-comp");
        tp.setExpanded(true);
        tp.setCollapsible(false);
        return new SimpleCompStructure<>(tp);
    }
}
